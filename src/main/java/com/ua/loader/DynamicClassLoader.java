package com.ua.loader;

import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.*;

/**
 * @author yaroslav.gryniuk
 */
class DynamicClassLoader {
    Map<String, String> paths = new HashMap<String, String>();
    private static final String FILE_SEPARATOR = System.getProperty("file.separator");

    public DynamicClassLoader() {
        this(null);
    }

    public DynamicClassLoader(List<String> packages) {
        if (packages == null) {
            final File f = new File(DynamicClassLoader.class.getProtectionDomain().getCodeSource().getLocation().getPath());
            paths = initPathsMap(bfs(f));
        } else {
            for (String packageName : packages) {
                File f = new File((DynamicClassLoader.class.getProtectionDomain().getCodeSource().getLocation().getPath() + packageName.replace(".", FILE_SEPARATOR)));
                paths.putAll(initPathsMap(bfs(f)));
            }
        }
    }

    /**
     * This method inits mappings of class names , to their package paths
     *
     * @param foundClasses
     * @return
     */
    private Map<String, String> initPathsMap(List<String> foundClasses) {
        Map<String, String> paths = new HashMap<String, String>();
        for (String osPath : foundClasses) {
            String packagePath = osPath.substring(osPath.indexOf("com" + FILE_SEPARATOR), osPath.length() - 6)
                    .replace(FILE_SEPARATOR, ".");
            String[] tokens = packagePath.split("\\.");
            paths.put(tokens[tokens.length - 1], packagePath);
        }
        return paths;
    }


    /**
     * Try to find class file with name , that match searchingClassName
     *
     * @param searchingClassName
     * @return
     * @throws ClassNotFoundException
     */
    public Class loadClass(String searchingClassName) throws ClassNotFoundException {
        String classPath = null;
        float maxMatchingCoeff = 0;
        for (String possibleClass : paths.keySet()) {
            float currentMatchingCoefficient = matchingCoefficient(possibleClass, searchingClassName);
            if (currentMatchingCoefficient > maxMatchingCoeff) {
                maxMatchingCoeff = currentMatchingCoefficient;
                classPath = paths.get(possibleClass);
            }
        }

        if (classPath == null)
            throw new ClassNotFoundException("cant found class in packages");
        return loadByDefaultClassLoader(classPath);
    }

    /**
     * Load class by class loader that load this class.
     *
     * @param path
     * @return class
     */
    private Class loadByDefaultClassLoader(String path) {
        ClassLoader classLoader = getClass().getClassLoader();
        Class aClass = null;
        try {
            aClass = classLoader.loadClass(path);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return aClass;
    }

    /**
     * This method ,using breadth-first search algorithm , find out all *.class files
     * (that contains Action or Expectation or Precondition),
     * that is not generated by javac (nested classes , anonymous classes ,etc)
     *
     * @param root
     * @return list of all *.class's path
     */

    private List<String> bfs(File root) {
        List<String> classFiles = new ArrayList<String>(20);
        Queue<File> filesQueue = new LinkedList<File>();
        filesQueue.add(root);
        while (!filesQueue.isEmpty()) {
            File currentRoot = filesQueue.poll();
            String[] nodes = currentRoot.list();
            for (String node : nodes) {
                File file = new File(currentRoot.getAbsoluteFile() + FILE_SEPARATOR + node);
                if (file.isDirectory())
                    filesQueue.add(file);
                else if (!file.getAbsolutePath().contains("$") && node.contains(".class"))
                    classFiles.add(file.getAbsolutePath());
            }
        }

        return classFiles;
    }

    /**
     * Matching coefficient is a number that represent ratio of same words
     * to the number of all words. For example :
     * matchingCoefficient("Lookup" , "LookupAction")  = 0.5
     * matchingCoefficient("Lookup" , "LookupRestAction")  = 0.3
     *
     * @return matchingCoefficient
     */

    float matchingCoefficient(String source, String test) {

        float res = 0;
        String[] testTokens = splitOnTokens(test);
        String[] sourceTokens = splitOnTokens(source);
        for (String testToken : testTokens) {
            for (String sourceToken : sourceTokens) {
                if (sourceToken.equalsIgnoreCase(testToken))
                    res++;
            }

        }
        int length = Math.max(sourceTokens.length, testTokens.length);
        return res / length;
    }

    /**
     * usage example
     * splitOnTokens(RemoveCache    with      restart MI) = [Remove, Cache, with, restart, MI]
     *
     * @param source
     * @return
     */

    static String[] splitOnTokens(String source) {
        List<String> newTokens = new ArrayList<String>();
        for (String token : StringUtils.splitByCharacterTypeCamelCase(source)) {
            if (token.trim().length() == 0)
                continue;
            newTokens.add(token);
        }
        return newTokens.toArray(new String[newTokens.size()]);
    }


}