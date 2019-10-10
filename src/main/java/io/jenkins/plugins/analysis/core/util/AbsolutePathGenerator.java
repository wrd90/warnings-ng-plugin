package io.jenkins.plugins.analysis.core.util;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;

import edu.hm.hafner.analysis.FilteredLog;
import edu.hm.hafner.analysis.Report;
import edu.hm.hafner.util.PathUtil;

/**
 * Resolves absolute paths of the affected files of a set of issues.
 *
 * @author Ullrich Hafner
 */
public class AbsolutePathGenerator {
    static final String NOTHING_TO_DO = "-> none of the issues requires resolving of absolute path";

    /**
     * Resolves absolute paths of the affected files of the specified set of issues.
     *
     * @param report
     *         the issues to resolve the paths
     * @param workspace
     *         the workspace containing the affected files
     * @param additionalPaths
     *         additional paths that may contain the affected files
     */
    public void run(final Report report, final Path workspace, final Collection<String> additionalPaths) {
        run(report, workspace, additionalPaths.stream().map(Paths::get).toArray(Path[]::new));
    }

    /**
     * Resolves absolute paths of the affected files of the specified set of issues.
     *
     * @param report
     *         the issues to resolve the paths
     * @param workspace
     *         the workspace containing the affected files
     * @param additionalPaths
     *         additional paths that may contain the affected files
     */
    public void run(final Report report, final Path workspace, final Path... additionalPaths) {
        Set<String> filesToProcess = report.getFiles()
                .stream()
                .filter(this::isInterestingFileName)
                .collect(Collectors.toSet());

        if (filesToProcess.isEmpty()) {
            report.logInfo(NOTHING_TO_DO);
            report.stream().forEach(issue -> issue.setFileName(new PathUtil().getAbsolutePath(issue.getFileName())));
            return;
        }

        FilteredLog log = new FilteredLog(report, "Can't resolve absolute paths for some files:");

        MutableList<Path> prefixes = Lists.mutable.with(additionalPaths).with(workspace);
        Map<String, String> pathMapping = resolveAbsoluteNames(filesToProcess, prefixes, log);
        report.stream()
                .filter(issue -> pathMapping.containsKey(issue.getFileName()))
                .forEach(issue -> issue.setFileName(pathMapping.get(issue.getFileName())));

        log.logSummary();
    }

    private boolean isInterestingFileName(final String fileName) {
        return !"-".equals(fileName) && !ConsoleLogHandler.isInConsoleLog(fileName);
    }

    private Map<String, String> resolveAbsoluteNames(final Set<String> affectedFiles, final MutableList<Path> prefixes,
            final FilteredLog log) {
        Map<String, String> pathMapping = new HashMap<>();
        int errors = 0;
        int unchanged = 0;
        int changed = 0;

        for (String fileName : affectedFiles) {
            Optional<String> absolutePath = resolveAbsolutePath(prefixes, fileName);
            if (absolutePath.isPresent()) {
                String resolved = absolutePath.get();
                pathMapping.put(fileName, resolved);
                if (fileName.equals(resolved)) {
                    unchanged++;
                }
                else {
                    changed++;
                }
            }
            else {
                log.logError("- %s", fileName);
                errors++;
            }
        }
        log.logInfo("-> %d resolved, %d unresolved, %d already resolved", changed, errors, unchanged);

        return pathMapping;
    }

    private Optional<String> resolveAbsolutePath(final MutableList<Path> prefixes, final String fileName) {
        return prefixes.stream()
                .map(path -> resolveAbsolutePath(path, fileName))
                .flatMap(o -> o.map(Stream::of).orElseGet(Stream::empty))
                .findFirst();
    }
    private Optional<String> resolveAbsolutePath(final Path parent, final String fileName) {
        try {
            return Optional.of(new PathUtil().toString(parent.resolve(fileName)));
        }
        catch (IOException | InvalidPathException ignored) {
            return Optional.empty();
        }
    }
}
