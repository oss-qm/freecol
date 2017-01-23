/**
 *  Copyright (C) 2002-2017   The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.freecol.common.io;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.filechooser.FileSystemView;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.util.OSUtils;
import static net.sf.freecol.common.util.CollectionUtils.*;
import static net.sf.freecol.common.util.StringUtils.*;
import net.sf.freecol.common.util.Utils;


/**
 * Simple container for the freecol file and directory structure model.
 */
public class FreeColDirectories {

    // No logger!  Many of these routines are called before logging is
    // initialized.

    private static final Comparator<File> fileNameComparator
        = Comparator.comparing(File::getName);


    private static final String AUTOSAVE_DIRECTORY = "autosave";

    private static final String BASE_DIRECTORY = "base";

    private static final String CLASSIC_DIRECTORY = "classic";

    private static final String[] CONFIG_DIRS
        = { "classic", "freecol" };

    private static final String DATA_DIRECTORY = "data";

    private static final String FREECOL_DIRECTORY = "freecol";

    private static final String HIGH_SCORE_FILE = "HighScores.xml";

    private static final String I18N_DIRECTORY = "strings";

    private static final String LOG_FILE = "FreeCol.log";

    private static final String MAPS_DIRECTORY = "maps";

    private static final String MESSAGE_FILE_PREFIX = "FreeColMessages";

    private static final String MESSAGE_FILE_SUFFIX = ".properties";

    private static final String MODS_DIRECTORY = "mods";

    private static final String MOD_FILE_SUFFIX = ".fmd";

    private static final String MOD_MESSAGE_FILE_PREFIX = "ModMessages";

    private static final String PLURALS_FILE_NAME = "plurals.xml";

    private static final String RESOURCE_FILE_PREFIX = "resources";

    private static final String RESOURCE_FILE_SUFFIX = ".properties";

    private static final String RULES_DIRECTORY = "rules";

    private static final String SAVE_GAME_SUFFIX = ".fsg";

    private static final String SAVE_DIRECTORY = "save";

    private static final String SPECIFICATION_FILE_NAME = "specification.xml";

    private static final String START_MAP_NAME = "startMap.fsg";

    private static final String SEPARATOR
        = System.getProperty("file.separator");

    private static final String TC_FILE_SUFFIX = ".ftc";

    private static final String ZIP_FILE_SUFFIX = ".zip";

    private static final String XDG_CONFIG_HOME_ENV = "XDG_CONFIG_HOME";
    private static final String XDG_CONFIG_HOME_DEFAULT = ".config";
    private static final String XDG_DATA_HOME_ENV = "XDG_DATA_HOME";
    private static final String XDG_DATA_HOME_DEFAULT = ".local/share";
    private static final String XDG_CACHE_HOME_ENV = "XDG_CACHE_HOME";
    private static final String XDG_CACHE_HOME_DEFAULT = ".cache";


    // Public names, used by the respective dialogs

    public static final String BASE_CLIENT_OPTIONS_FILE_NAME = "client-options.xml";

    public static final String CLIENT_OPTIONS_FILE_NAME = "options.xml";

    public static final String CUSTOM_DIFFICULTY_FILE_NAME = "custom.xml";

    public static final String GAME_OPTIONS_FILE_NAME = "game_options.xml";

    public static final String MAP_FILE_NAME = "my_map.fsg";

    public static final String MAP_GENERATOR_OPTIONS_FILE_NAME
        = "map_generator_options.xml";

    public static final String MOD_DESCRIPTOR_FILE_NAME = "mod.xml";

    /**
     * The directory containing automatically created save games.  At
     * program start, the path of this directory is based on the path
     * where to store regular save games.  If the saved game is
     * changed by the user during the game, then the value of
     * autosaveDirectory will not change.
     */
    private static File autosaveDirectory = null;

    /**
     * A file containing the client options.
     *
     * Can be overridden at the command line.
     */
    private static File clientOptionsFile = null;

    /**
     * The directory where the standard freecol data is installed.
     *
     * Can be overridden at the command line.
     *
     * FIXME: defaults lamely to ./data.  Do something better in the
     * installer.
     */
    private static File dataDirectory = new File(DATA_DIRECTORY);

    /**
     * The path to the log file.
     *
     * Can be overridden at the command line.
     */
    private static String logFilePath = null;

    /**
     * Where games are saved.
     *
     * Can be overridden in game or from the command line by
     * specifying the save game file.
     */
    private static File saveDirectory = null;

    /**
     * The current save game file.
     *
     * Can be modified in game.
     */
    private static File savegameFile = null;

    /**
     * The directory where freecol saves transient information.
     */
    private static File userCacheDirectory = null;

    /**
     * The directory where freecol saves user configuration.
     *
     * This will be set by default but can be overridden at the
     * command line.
     */
    private static File userConfigDirectory = null;

    /**
     * The directory where freecol saves user data.
     *
     * This will be set by default but can be overridden at the
     * command line.
     */
    private static File userDataDirectory = null;

    /**
     * An optional directory containing user mods.
     */
    private static File userModsDirectory = null;


    /**
     * Does the OS look like Mac OS X?
     *
     * @return True if Mac OS X appears to be present.
     */
    public static boolean onMacOSX() {
        return "Mac OS X".equals(OSUtils.getOperatingSystem());
    }

    /**
     * Does the OS look like some sort of unix?
     *
     * @return True we hope.
     */
    public static boolean onUnix() {
        return "/".equals(SEPARATOR);
    }

    /**
     * Does the OS look like some sort of Windows?
     *
     * @return True if Windows appears to be present.
     */
    public static boolean onWindows() {
        return OSUtils.getOperatingSystem().startsWith("Windows");
    }

    /**
     * Get the user home directory.
     *
     * @return The user home directory.
     */
    private static File getUserDefaultDirectory() {
        return FileSystemView.getFileSystemView().getDefaultDirectory();
    }

    /**
     * Check a directory for read and write access.
     *
     * @param dir The {@code File} that must be a usable directory.
     * @return Null on success, an error message key on failure.
     */
    public static String checkDir(File dir) {
        return (dir == null || !dir.exists()) ? "cli.error.home.notExists"
            : (!dir.isDirectory()) ? "cli.error.home.notDir"
            : (!dir.canRead()) ? "cli.error.home.noRead"
            : (!dir.canWrite()) ? "cli.error.home.noWrite"
            : null;
    }

    /**
     * Get directories for XDG compliant systems.
     *
     * Result is:
     * - Negative if a non-XDG OS is detected or there is insufficient
     *   XDG structure to merit migrating, or what structure there is is
     *   broken in some way.
     * - Zero if there is at least one relevant XDG environment
     *   variable in use and it points to a valid writable directory,
     *   or the default exists and is writable.
     * - Positive if there are a full set of suitable XDG directories and
     *   there are freecol directories therein.
     * - Otherwise negative, including non-directories in the wrong place
     *   and unwritable directories.
     *
     * The intent is to ignore XDG on negative, migrate on zero, and use
     * on positive.
     *
     * @param dirs An array of {@code File} to be filled in with the
     *     XDG directory if it is present or created.
     * @return The XDG compliance state.
     */
    private static int getXDGDirs(File[] dirs) {
        if (onMacOSX() || onWindows() || !onUnix()) return -1;

        int ret = -1;
        File home = getUserDefaultDirectory();
        if (home == null) return -1; // Fail badly
        String[][] xdg = { { XDG_CONFIG_HOME_ENV, XDG_CONFIG_HOME_DEFAULT },
                           { XDG_DATA_HOME_ENV,   XDG_DATA_HOME_DEFAULT },
                           { XDG_CACHE_HOME_ENV,  XDG_CACHE_HOME_DEFAULT } };
        File[] todo = new File[xdg.length];
        for (int i = 0; i < xdg.length; i++) {
            String env = System.getenv(xdg[i][0]);
            File d = (env != null) ? new File(env) : new File(home, xdg[i][1]);
            if (d.exists()) {
                if (!d.isDirectory() || !d.canWrite()) {
                    return -1; // Fail hard if something is broken
                }
                ret = Math.max(ret, 0);
                File f = new File(d, FREECOL_DIRECTORY);
                if (f.exists()) {
                    if (!f.isDirectory() || !f.canWrite()) {
                        return -1; // Again, fail hard
                    }
                    dirs[i] = f;
                    todo[i] = null;
                    ret++;
                } else {
                    dirs[i] = d;
                    todo[i] = f;
                }
            } else {
                dirs[i] = null;
                todo[i] = d;
            }
        }
        if (ret < 0) return -1; // No evidence of interest in XDG standard
        if (ret == xdg.length) return 1; // Already fully XDG compliant

        // Create the directories for migration
        for (int i = 0; i < xdg.length; i++) {
            if (todo[i] != null) {
                if (!todo[i].getPath().endsWith(FREECOL_DIRECTORY)) {
                    if (!todo[i].mkdir()) return -1;
                    todo[i] = new File(todo[i], FREECOL_DIRECTORY);
                }
                if (!todo[i].mkdir()) return -1;
                dirs[i] = todo[i];
            }
        }
        return 0;
    }

    /**
     * Is the specified file a writable directory?
     *
     * @param f The {@code File} to check.
     * @return True if the file is a writable directory.
     */
    private static boolean isGoodDirectory(File f) {
        return f.exists() && f.isDirectory() && f.canWrite();
    }

    /**
     * Create the given directory if it does not exist, otherwise expect
     * it to be writable.
     *
     * @param dir The {@code File} specifying the required directory.
     * @return The required directory, or null on failure.
     */
    private static File requireDir(File dir) {
        if (dir.exists()) {
            if (dir.isDirectory() && dir.canWrite()) return dir;
        } else {
            if (dir.mkdir()) return dir;
        }
        return null;
    }

    /**
     * Get FreeCol directories for MacOSX.
     *
     * No separate cache directory here.
     *
     * Result is:
     * - Negative on failure.
     * - Zero if a migration is needed.
     * - Positive if no migration is needed.
     *
     * @param dirs An array of {@code File} to be filled in with the
     *     MacOSX freecol directories if present or created.
     * @return The migration state.
     */
    private static int getMacOSXDirs(File[] dirs) {
        if (!onMacOSX()) return -1;
        int ret = 0;
        File homeDir = getUserDefaultDirectory();
        if (homeDir == null) return -1;
        File libDir = new File(homeDir, "Library");
        if (!isGoodDirectory(libDir)) return -1;

        if (dirs[0] == null) {
            File prefsDir = new File(libDir, "Preferences");
            if (isGoodDirectory(prefsDir)) {
                dirs[0] = prefsDir;
                File d = new File(prefsDir, FREECOL_DIRECTORY);
                if (d.exists()) {
                    if (d.isDirectory() && d.canWrite()) {
                        dirs[0] = d;
                        ret++;
                    } else return -1;
                }
            } else return -1;
        }

        if (dirs[1] == null) {
            File appsDir = new File(libDir, "Application Support");
            if (isGoodDirectory(appsDir)) {
                dirs[1] = appsDir;
                File d = new File(appsDir, FREECOL_DIRECTORY);
                if (d.exists()) {
                    if (d.isDirectory() && d.canWrite()) {
                        dirs[1] = d;
                        ret++;
                    } else return -1;
                }
            } else return -1;
        }

        if (dirs[2] == null) {
            dirs[2] = dirs[1];
        }

        if (ret == 2) return 1;

        File d = requireDir(new File(dirs[0], FREECOL_DIRECTORY));
        if (d == null) return -1;
        dirs[0] = d;

        d = requireDir(new File(dirs[1], FREECOL_DIRECTORY));
        if (d == null) return -1;
        dirs[1] = d;

        return 0;
    }


    /**
     * Get FreeCol directories for Windows.
     *
     * Simple case, everything is in the one directory.
     *
     * Result is:
     * - Negative on failure.
     * - Zero if a migration is needed.
     * - Positive if no migration is needed.
     *
     * @param dirs An array of {@code File} to be filled in with the
     *     Windows freecol directories if present or created.
     * @return The migration state.
     */
    private static int getWindowsDirs(File[] dirs) {
        if (onMacOSX() || !onWindows() || onUnix()) return -1;

        File home = getUserDefaultDirectory();
        if (home == null) return -1; // Fail badly
        File d = requireDir(new File(home, FREECOL_DIRECTORY));
        if (d == null) return -1;
        dirs[0] = dirs[1] = dirs[2] = d;
        return 1; // Do not migrate windows
    }

    /**
     * Find the old user directory.
     *
     * Does not try to be clever, just tries ~/FreeCol, ~/.freecol, and
     * ~/Library/FreeCol which should find the old directory on the three
     * known systems.
     *
     * @return The old user directory, or null if none found.
     */
    private static File getOldUserDirectory() {
        File home = getUserDefaultDirectory();
        File old = new File(home, "FreeCol");
        if (old.exists() && old.isDirectory() && old.canRead()) return old;
        old = new File(home, ".freecol");
        if (old.exists() && old.isDirectory() && old.canRead()) return old;
        old = new File(home, "Library");
        if (old.exists() && old.isDirectory() && old.canRead()) {
            old = new File(old, "FreeCol");
            if (old.exists() && old.isDirectory() && old.canRead()) return old;
        }
        return null;
    }

    /**
     * Copy directory with given name under an old directory to a new
     * directory.
     *
     * @param oldDir The old directory.
     * @param name The name of the directory to copy.
     * @param newDir The new directory.
     */
    private static void copyIfFound(File oldDir, String name, File newDir) {
        File src = new File(oldDir, name);
        File dst = new File(newDir, name);
        if (src.exists() && src.isDirectory() && !dst.exists()) {
            try {
                Files.copy(src.toPath(), dst.toPath(),
                    StandardCopyOption.COPY_ATTRIBUTES);
            } catch (IOException ioe) {
                System.err.println("Could not copy " + src.toString() + " to "
                    + dst.toString() + ": " + ioe.getMessage());
            }
        }
    }

    /**
     * Insist that a directory either already exists, or is created.
     *
     * @param file A {@code File} specifying where to make the directory.
     * @return True if the directory is now there.
     */
    private static boolean insistDirectory(File file) {
        boolean ret;
        if (file.exists()) {
            if (!(ret = file.isDirectory())) {
                System.err.println("Could not create directory "
                    + file.getPath() + " because a non-directory with that name is already there.");
            }
        } else {
            try {
                ret = file.mkdir();
            } catch (Exception e) {
                ret = false;
                System.err.println("Could not make directory " + file.getPath()
                    + ": " + e.getMessage());
            }
        }
        return ret;
    }

    /**
     * Derive the directory for the autosave files from the save directory.
     */
    private static void deriveAutosaveDirectory() {
        File dir;
        if (autosaveDirectory == null && saveDirectory != null
            && (dir = new File(saveDirectory, AUTOSAVE_DIRECTORY)) != null
            && insistDirectory(dir)) {
            autosaveDirectory = dir;
        }
    }

    /**
     * Collect files from a directory that match a predicate.
     *
     * @param dir The directory to load from.
     * @param pred A {@code Predicate} to match files with.
     * @return A list of {@code File}s.
     */
    private static List<File> collectFiles(File dir, Predicate<File> pred) {
        return transform(fileStream(dir), pred, Function.<File>identity(),
                         fileNameComparator);
    }


    // Main initialization/bootstrap routines.
    // These need to be called early before the subsidiary directory
    // accessors are used.

    /**
     * Sets the data directory.
     *
     * Insist that the base resources and i18n subdirectories are present.
     *
     * @param path The path to the new data directory, or null to
     *     apply the default.
     * @return A (non-i18n) error message on failure, null on success.
     */
    public static String setDataDirectory(String path) {
        if (path == null) path = DATA_DIRECTORY;
        File dir = new File(path);
        if (!dir.isDirectory()) return "Not a directory: " + path;
        if (!dir.canRead()) return "Can not read directory: " + path;
        dataDirectory = dir;
        return null;
    }

    /**
     * Checks/creates the freecol directory structure for the current
     * user.
     *
     * The main user directory used to be in the current user's home
     * directory, and called ".freecol" (UNIXes including Mac in
     * 0.9.x) or "freecol" or even FreeCol.  Now we use:
     *
     * - on XDG standard compliant Unixes:
     *   - config:  ~/.config/freecol
     *   - data:    ~/.local/share/freecol
     *   - logging: ~/.cache/freecol
     * - on Mac:
     *   - config:  ~/Library/Preferences/freecol
     *   - else:    ~/Library/Application Support/freecol
     * - on Windows:
     *   - everything in <em>default directory</em>/freecol
     * - otherwise use what was there
     *
     * Note: the freecol data directory is set independently and earlier
     * in initialization than this routine.
     *
     * FIXME: Should the default location of the main user and data
     * directories be determined by the installer?
     *
     * @return A message key to use to create a message to the user
     *     possibly describing any directory migration, or null if
     *     nothing to say.
     */
    public static synchronized String setUserDirectories() {
        if (userConfigDirectory != null
            && !isGoodDirectory(userConfigDirectory))
            userConfigDirectory = null;
        if (userDataDirectory != null
            && !isGoodDirectory(userDataDirectory))
            userDataDirectory = null;
        if (userCacheDirectory != null
            && !isGoodDirectory(userCacheDirectory))
            userCacheDirectory = null;
        File dirs[] = { userConfigDirectory, userDataDirectory,
                        userCacheDirectory };

        // If the CL-specified directories are valid, all is well.
        // Check for OSX next because it is a Unix.
        int migrate = (dirs[0] != null && isGoodDirectory(dirs[0])
            && dirs[1] != null && isGoodDirectory(dirs[1])
            && dirs[2] != null && isGoodDirectory(dirs[2])) ? 1
            : (onMacOSX()) ? getMacOSXDirs(dirs)
            : (onUnix()) ? getXDGDirs(dirs)
            : (onWindows()) ? getWindowsDirs(dirs)
            : -1;
        File oldDir = getOldUserDirectory();
        if (migrate < 0) {
            if (oldDir == null) return "main.userDir.fail";
            dirs[0] = dirs[1] = dirs[2] = oldDir; // Do not migrate.
            migrate = 1;
        }

        // Only set user directories if not already overridden at the
        // command line, and do not migrate in such cases.
        if (userConfigDirectory == null) {
            userConfigDirectory = dirs[0];
        } else migrate = 1;
        if (userDataDirectory == null) {
            userDataDirectory = dirs[1];
        } else migrate = 1;
        if (userCacheDirectory == null) {
            userCacheDirectory = dirs[2];
        } else migrate = 1;
        if (migrate == 0 && oldDir != null) {
            copyIfFound(oldDir, "classic", userConfigDirectory);
            copyIfFound(oldDir, "freecol", userConfigDirectory);
            copyIfFound(oldDir, "save",    userDataDirectory);
            copyIfFound(oldDir, "mods",    userDataDirectory);
        }

        if (logFilePath == null) {
            logFilePath = getUserCacheDirectory() + SEPARATOR + LOG_FILE;
        }

        if (saveDirectory == null) {
            saveDirectory = new File(getUserDataDirectory(), SAVE_DIRECTORY);
            if (!insistDirectory(saveDirectory)) return "main.userDir.fail";
        }
        deriveAutosaveDirectory();

        userModsDirectory = new File(getUserDataDirectory(), MODS_DIRECTORY);
        if (!insistDirectory(userModsDirectory)) userModsDirectory = null;

        return (migrate > 0) ? null
            : (onMacOSX())  ? "main.userDir.macosx"
            : (onUnix())    ? "main.userDir.unix"
            : (onWindows()) ? "main.userDir.windows"
            : null;
    }

    /**
     * Remove disallowed parts of a user supplied file name.
     *
     * @param The input file name.
     * @return A sanitized file name.
     */
    public static String sanitize(String fileName) {
        StringBuilder strings = new StringBuilder();
        for (int i = 0; i < fileName.length(); i++) {
            String s = fileName.substring(i, i+1);
            if (SEPARATOR.equals(s)) continue;
            strings.append(s);
        }
        return strings.toString();
    }


    // Directory accessors.
    // Where there are supported command line arguments there will also
    // be a mutator.

    /**
     * Gets the directory where the automatically saved games should be put.
     *
     * @return The autosave directory.
     */
    public static File getAutosaveDirectory() {
        return autosaveDirectory;
    }

    /**
     * Get a specific autosave file.
     *
     * @param fileName The name of the file.
     * @return The {@code File} found.
     */
    public static File getAutosaveFile(String fileName) {
        return new File(getAutosaveDirectory(), sanitize(fileName));
    }

    public static final String FREECOL_SAVE_SUFFIX = "." + FreeCol.FREECOL_SAVE_EXTENSION;

    /**
     * Remove all autosave files.
     *
     * @param prefix The autosave file prefix.
     */
    public static void removeAutosaves(String prefix) {
        File asd = getAutosaveDirectory();
        for (String n : asd.list())
            if (n.startsWith(prefix))
                Utils.deleteFile(new File(asd, n));
    }

    /**
     * Gets the base resources directory.
     *
     * @return The base resources directory.
     */
    public static File getBaseDirectory() {
        return new File(getDataDirectory(), BASE_DIRECTORY);
    }

    /**
     * Gets the base client options file.
     *
     * @return The base client options file.
     */
    public static File getBaseClientOptionsFile() {
        return new File(getBaseDirectory(), BASE_CLIENT_OPTIONS_FILE_NAME);
    }

    /**
     * Gets the file containing the client options.
     *
     * @return The client options file, if any.
     */
    public static File getClientOptionsFile() {
        return (clientOptionsFile != null) ? clientOptionsFile
            : getOptionsFile(CLIENT_OPTIONS_FILE_NAME);
    }

    /**
     * Sets the client options file.
     *
     * @param path The new client options file.
     * @return True if the file was set successfully.
     */
    public static boolean setClientOptionsFile(String path) {
        File file = new File(path);
        if (file.exists() && file.isFile() && file.canRead()) {
            clientOptionsFile = file;
            return true;
        }
        return false;
    }

    /**
     * Get a compatibility file.
     *
     * Not sanitizing the file name as it is fixed in all current uses.
     *
     * @param fileName The name of the compatibility file.
     * @return The {@code File} found.
     */
    public static File getCompatibilityFile(String fileName) {
        return new File(getBaseDirectory(), fileName);
    }

    /**
     * Gets the data directory.
     *
     * @return The directory where the data files are located.
     */
    public static File getDataDirectory() {
        return dataDirectory;
    }

    /**
     * Get the debug-run save file.
     *
     * @return The save {@code File}, if any.
     */
    public static File getDebugRunSaveFile() {
        return new File(System.getProperty("user.dir"),
                        FreeColDebugger.getDebugRunSave());
    }

    /**
     * Gets the high score file.
     *
     * @return The high score file, if it exists.
     */
    public static File getHighScoreFile() {
        return new File(getUserDataDirectory(), HIGH_SCORE_FILE);
    }

    /**
     * Gets the directory containing language property files.
     *
     * @return The FreeCol i18n directory.
     */
    public static File getI18nDirectory() {
        return new File(getDataDirectory(), I18N_DIRECTORY);
    }

    /**
     * Get the contents of the log file.
     *
     * @return A string of the log file contents, or null on error.
     */
    public static String getLogFileContents() {
        return Utils.getUTF8Contents(new File(getLogFilePath()));
    }

    /**
     * Get a list of candidate message file names for a given locale.
     *
     * @param locale The {@code Locale} to generate file names for.
     * @return A list of message {@code File}s.
     */
    public static List<File> getI18nMessageFileList(Locale locale) {
        List<File> result = new ArrayList<>();
        File i18nDirectory = getI18nDirectory();
        for (String name : getMessageFileNameList(locale)) {
            File f = new File(i18nDirectory, name);
            if (f.canRead()) result.add(f);
        }
        return result;
    }

    /**
     * Get the i18n plurals file.
     *
     * @return The plurals {@code File}.
     */
    public static File getI18nPluralsFile() {
        return new File(getI18nDirectory(), PLURALS_FILE_NAME);
    }

    /**
     * Get a list of all the supported language identifiers.
     *
     * @return A list of language identifiers for which there is an
     *     i18n-message file.
     */
    public static List<String> getLanguageIdList() {
        File[] files = getI18nDirectory().listFiles();
        return (files == null) ? Collections.<String>emptyList()
            : transform(files, f -> f.canRead(), f -> getLanguageId(f));
    }

    /**
     * If this a messages file, work out which language identifier it
     * belongs to.
     *
     * @param file The {@code File} to test.
     * @return The language identifier found, or null on failure.
     */
    public static String getLanguageId(File file) {
        if (file == null) return null;
        final String name = file.getName();
        // Make sure it is at least a messages file.
        if (name == null
            || !name.startsWith(MESSAGE_FILE_PREFIX)
            || !name.endsWith(MESSAGE_FILE_SUFFIX)) return null;
        String languageId = name.substring(MESSAGE_FILE_PREFIX.length(),
            name.length() - MESSAGE_FILE_SUFFIX.length());
        return ("".equals(languageId)) ? "en" // FreeColMessages.properties
            : ("_qqq".equals(languageId)) ? null // qqq is explanations only
            : (languageId.startsWith("_")) ? languageId.substring(1)
            : languageId;
    }

    /**
     * Gets a list containing the names of all possible message files
     * for a locale.
     *
     * @param prefix The file name prefix.
     * @param suffix The file name suffix.
     * @param locale The {@code Locale} to generate file names for.
     * @return A list of candidate file names.
     */
    public static List<String> getLocaleFileNames(String prefix,
                                                  String suffix,
                                                  Locale locale) {
        String language = locale.getLanguage();
        String country = locale.getCountry();
        String variant = locale.getVariant();

        List<String> result = new ArrayList<>(4);

        if (!language.isEmpty()) language = "_" + language;
        if (!country.isEmpty()) country = "_" + country;
        if (!variant.isEmpty()) variant = "_" + variant;

        result.add(prefix + suffix);
        String filename = prefix + language + suffix;
        if (!result.contains(filename)) result.add(filename);
        filename = prefix + language + country + suffix;
        if (!result.contains(filename)) result.add(filename);
        filename = prefix + language + country + variant + suffix;
        if (!result.contains(filename)) result.add(filename);
        return result;
    }

    /**
     * Gets the log file path.
     *
     * @return The log file path.
     */
    public static String getLogFilePath() {
        return logFilePath;
    }

    /**
     * Sets the log file path.
     *
     * @param path The new log file path.
     */
    public static void setLogFilePath(String path) {
        logFilePath = path;
    }

    /**
     * Gets a new log writer.
     *
     * @return The log {@code Writer}.
     * @exception FreeColException if there was a problem creating the writer.
     */
    public static Writer getLogWriter() throws FreeColException {
        String path = getLogFilePath();
        File file = new File(path);
        if (file.exists()) {
            if (file.isDirectory()) {
                throw new FreeColException("Log file \"" + path
                    + "\" could not be created.");
            } else if (file.isFile()) {
                try {
                    file.delete();
                } catch (SecurityException ex) {} // Do what?
            }
        }
        try {
            file.createNewFile();
        } catch (IOException e) {
            throw new FreeColException("Log file \"" + path
                + "\" could not be created.", e);
        }
        if (!file.canWrite()) {
            throw new FreeColException("Can not write in log file \""
                + path + "\".");
        }
        Writer writer = Utils.getFileUTF8Writer(file);
        if (writer == null) {
            throw new FreeColException("Can not create writer for log file \""
                + path + "\".");
        }
        return writer;
    }

    /**
     * Gets the directory containing the predefined maps.
     *
     * @return The predefined maps.
     */
    public static File getMapsDirectory() {
        return new File(getDataDirectory(), MAPS_DIRECTORY);
    }

    public static boolean checkSavegameFile(File f) {
        return f.isFile() && f.canRead()
            && f.getName().endsWith(SAVE_GAME_SUFFIX);
    }

    /**
     * Get the map files.
     *
     * @return A list of map files, or null on error.
     */
    public static List<File> getMapFileList() {
        return getSavegameFileList(getMapsDirectory());
    }

    /**
     * Get the message file names for a given locale.
     *
     * @param locale The {@code Locale} to generate names for.
     * @return A list of potential message file names.
     */
    public static List<String> getMessageFileNameList(Locale locale) {
        return getLocaleFileNames(MESSAGE_FILE_PREFIX, MESSAGE_FILE_SUFFIX,
                                  locale);
    }

    /**
     * Get the mod message file names for a given locale.
     *
     * @param locale The {@code Locale} to generate names for.
     * @return A list of potential mod message file names.
     */
    public static List<String> getModMessageFileNames(Locale locale) {
        return getLocaleFileNames(MOD_MESSAGE_FILE_PREFIX, MESSAGE_FILE_SUFFIX,
                                  locale);
    }

    private static boolean checkModFile(File f) {
        return (Utils.fileAnySuffix(f, MOD_FILE_SUFFIX, ZIP_FILE_SUFFIX)
             || Utils.directoryAllPresent(f, MOD_DESCRIPTOR_FILE_NAME));
    }

    /**
     * Get a list of the standard and current user mod files.
     *
     * @return A list of mod {@code File}s.
     */
    public static List<File> getModFileList() {
        List<File> ret = new ArrayList<>();
        for (File f : getStandardModsDirectory().listFiles())
            if (checkModFile(f)) ret.add(f);

        for (File f : getUserModsDirectory().listFiles())
            if (checkModFile(f)) ret.add(f);

        return ret;
    }

    /**
     * Gets the directory where the user options are saved.
     *
     * @return The directory to save user options in.
     */
    public static File getOptionsDirectory() {
        File dir = new File(getUserConfigDirectory(), FreeCol.getTC());
        return (insistDirectory(dir)) ? dir : null;
    }

    /**
     * Get an options file from the options directory.
     *
     * @param fileName The name of the file within the options directory.
     * @return The options file.
     */
    public static File getOptionsFile(String fileName) {
        File dir = getOptionsDirectory();
        return (dir == null) ? null : new File(dir, sanitize(fileName));
    }

    /**
     * Get a list of candidate resource file names for a given locale.
     *
     * @return A list of resource file names.
     */
    public static List<String> getResourceFileNames() {
        return getLocaleFileNames(RESOURCE_FILE_PREFIX, RESOURCE_FILE_SUFFIX,
                                  Locale.getDefault());
    }

    /**
     * Gets the directory containing the classic rules.
     *
     * @return The classic rules directory.
     */
    public static File getRulesClassicDirectory() {
        return new File(getRulesDirectory(), CLASSIC_DIRECTORY);
    }

    /**
     * Gets the directory containing the various rulesets.
     *
     * @return The ruleset directory.
     */
    public static File getRulesDirectory() {
        return new File(getDataDirectory(), RULES_DIRECTORY);
    }

    /**
     * Gets the directory where the savegames should be put.
     *
     * @return The save directory.
     */
    public static File getSaveDirectory() {
        return saveDirectory;
    }

    /**
     * Gets the save game file.
     *
     * @return The save game file.
     */
    public static File getSavegameFile() {
        return savegameFile;
    }

    /**
     * Gets the save game files in a given directory.
     *
     * @param directory The base directory, or the default locations if null.
     * @return A list of save game {@code File}s.
     */
    public static List<File> getSavegameFileList(File directory) {
        if ((directory == null) || (!directory.isDirectory()))
            return Collections.<File>emptyList();

        List<File> result = new ArrayList<>();
        for (File walk : directory.listFiles())
            if (checkSavegameFile(walk))
                result.add(walk);

        Collections.sort(result);
        return result;
    }

    /**
     * Sets the save game file.
     *
     * @param path The path to the new save game file.
     * @return True if the setting succeeds.
     */
    public static boolean setSavegameFile(String path) {
        File file = new File(path);
        if (!file.exists() || !file.isFile() || !file.canRead()) {
            file = new File(getSaveDirectory(), path);
            if (!file.exists() || !file.isFile() || !file.canRead()) return false;
        }
        savegameFile = file;
        File parent = file.getParentFile();
        if (parent == null) parent = new File(".");
        saveDirectory = parent;
        deriveAutosaveDirectory();
        return true;
    }

    /**
     * Gets the most recently saved game file, or <b>null</b>.  (This
     * may be either from a recent arbitrary user operation or an
     * autosave function.)
     *
     * @return The recent save game {@code File}, or null if not found.
     */
    public static File getLastSaveGameFile() {
        long last_mtime = -1;
        File last_file = null;

        for (File walk : getSaveDirectory().listFiles()) {
            if (checkSavegameFile(walk)) {
                long mtime = walk.lastModified();
                if (mtime > last_mtime) {
                    last_mtime = mtime;
                    last_file = walk;
                }
            }
        }

        for (File walk : getAutosaveDirectory().listFiles()) {
            if (checkSavegameFile(walk)) {
                long mtime = walk.lastModified();
                if (mtime > last_mtime) {
                    last_mtime = mtime;
                    last_file = walk;
                }
            }
        }

        return last_file;
    }

    /**
     * Gets the standard mods directory.
     *
     * @return The directory where the standard mods are located.
     */
    public static File getStandardModsDirectory() {
        return new File(getDataDirectory(), MODS_DIRECTORY);
    }

    /**
     * Get the map file to start from, if any.
     *
     * @return The start map file if any.
     */
    public static File getStartMapFile() {
        return new File(getAutosaveDirectory(), START_MAP_NAME);
    }

    /**
     * Get all available rules files.
     *
     * @return A list of {@code File}s containing rulesets.
     */
    public static List<File> getTcFileList() {
        List<File> result = new ArrayList<File>();
        for (File f : getRulesDirectory().listFiles())
            if (Utils.fileAnySuffix(f, TC_FILE_SUFFIX, ZIP_FILE_SUFFIX)
             || Utils.directoryAllPresent(f, MOD_DESCRIPTOR_FILE_NAME, SPECIFICATION_FILE_NAME))
                result.add(f);
        Collections.sort(result);
        return result;
    }

    /**
     * Gets the user cache directory, that is the directory under which
     * the transient user files live.
     *
     * @return The user cache directory.
     */
    public static File getUserCacheDirectory() {
        return userCacheDirectory;
    }

    /**
     * Sets the user cache directory, that is the directory under which
     * the user-specific cache files live.
     *
     * @param path The path to the new user cache directory.
     * @return Null on success, an error message key on failure.
     */
    public static String setUserCacheDirectory(String path) {
        File dir = new File(path);
        String ret = checkDir(dir);
        if (ret == null) userCacheDirectory = dir;
        return ret;
    }

    /**
     * Gets the user config directory, that is the directory under which
     * the user-specific config files live.
     *
     * @return The user config directory.
     */
    public static File getUserConfigDirectory() {
        return userConfigDirectory;
    }

    /**
     * Sets the user config directory, that is the directory under which
     * the user-specific config files live.
     *
     * @param path The path to the new user config directory.
     * @return Null on success, an error message key on failure.
     */
    public static String setUserConfigDirectory(String path) {
        File dir = new File(path);
        String ret = checkDir(dir);
        if (ret == null) userConfigDirectory = dir;
        return ret;
    }

    /**
     * Gets the user data directory, that is the directory under which
     * the user-specific data lives.
     *
     * @return The user data directory.
     */
    public static File getUserDataDirectory() {
        return userDataDirectory;
    }

    /**
     * Sets the main user data directory, creating it if necessary.
     * If pre-existing, it must be a directory, readable and writable.
     *
     * @param path The path to the new main data user directory, or
     *     null to apply the default.
     * @return Null on success, an error message key on failure.
     */
    public static String setUserDataDirectory(String path) {
        File dir = new File(path);
        String ret = checkDir(dir);
        if (ret == null) userDataDirectory = dir;
        return ret;
    }

    /**
     * Gets the user mods directory.
     *
     * @return The directory where user mods are located, or null if none.
     */
    public static File getUserModsDirectory() {
        return userModsDirectory;
    }
}
