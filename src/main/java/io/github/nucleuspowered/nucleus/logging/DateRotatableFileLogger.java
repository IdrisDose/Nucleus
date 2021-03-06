/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.logging;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.Util;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.function.Function;

public class DateRotatableFileLogger implements Closeable {

    private final static Path nucleusBase = Paths.get("logs/plugin");

    private final Path directory;
    private final String filenamePrefix;
    private Instant currentDate;
    private LogFile file;
    private final Function<String, String> formatter;
    private boolean isClosed = false;

    public DateRotatableFileLogger(String directory, String filenamePrefix, Function<String, String> formatter) throws IOException {
        Preconditions.checkNotNull(directory);
        Preconditions.checkNotNull(filenamePrefix);

        this.directory = nucleusBase.resolve(directory);
        this.filenamePrefix = filenamePrefix;
        this.formatter = formatter == null ? s -> s : formatter;
        Files.createDirectories(this.directory);
    }

    private void openFile() throws IOException {
        if (isClosed) {
            throw new IllegalStateException();
        }

        if (file != null && !file.isClosed()) {
            try {
                file.close();
            } finally {
                file = null;
            }
        }

        int count = 0;
        boolean go = false;
        String fileName;
        do {
            count++;
            fileName = directory.toString() + "/" + filenamePrefix + "-" + DateTimeFormatter.ofPattern("yyyy-MM-dd").format(Instant.now().atZone(ZoneId.systemDefault())) + "-" + count + ".log";
            Path nextFile = Paths.get(fileName);
            if (Files.exists(nextFile)) {
                try {
                    Util.compressAndDeleteFile(nextFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (!Files.exists(Paths.get(fileName + ".gz"))) {
                file = new LogFile(nextFile, formatter);
                go = true;
            }
        } while(!go);

        currentDate = Instant.now().truncatedTo(ChronoUnit.DAYS);
    }

    public void logEntry(String entry) throws IOException {
        if (isClosed) {
            throw new IllegalStateException();
        }

        logEntry(Lists.newArrayList(entry), true);
    }

    public void logEntry(Iterable<String> entry) throws IOException {
        if (isClosed) {
            throw new IllegalStateException();
        }

        logEntry(entry, true);
    }

    private void logEntry(Iterable<String> entry, boolean retryOnError) throws IOException {
        if (file == null || file.isClosed() || Instant.now().truncatedTo(ChronoUnit.DAYS).isAfter(currentDate)) {
            openFile();
        }

        try {
            Iterator<String> iterator = entry.iterator();
            while (iterator.hasNext()) {
                file.writeLine(iterator.next());
                iterator.remove();
            }

            file.flush();
        } catch (IOException e) {
            if (retryOnError) {
                logEntry(entry, false);
            } else {
                throw e;
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (isClosed) {
            return;
        }

        if (file != null && !file.isClosed()) {
            file.close();
            file = null;
            this.isClosed = true;
        }
    }
}
