package io.clownfish.clownfish.compiler;

import io.clownfish.clownfish.beans.LoginBean;
import io.clownfish.clownfish.dbentities.CfJava;
import io.clownfish.clownfish.serviceinterface.CfJavaService;
import io.clownfish.clownfish.utils.PropertyUtil;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.tools.*;
import java.io.*;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Named("javaCompiler")
@Scope("singleton")
@Component
public class CfClassCompiler
{
    @Inject LoginBean loginbean;
    private static CfClassLoader cfclassLoader;
    private static PropertyUtil propertyUtil;
    private static CfJavaService cfjavaService;
    private @Getter @Setter List<CfJava> javaListSelected;
    private @Getter @Setter Map<Class<?>, List<Method>> classMethodMap = new HashMap<>();
    private static @Getter @Setter Path tmpdir;
    @Getter @Setter StringWriter compileOut;
    @Getter @Setter boolean verboseCompile = true;

    final transient org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CfClassCompiler.class);

    public CfClassCompiler() {}
    
    public void init(CfClassLoader cfclassLoader_, PropertyUtil propertyUtil_, CfJavaService cfjavaService_)
    {
        cfclassLoader = cfclassLoader_;
        propertyUtil = propertyUtil_;
        cfjavaService = cfjavaService_;
    }

    //TODO: Custom ClassLoader with the ability to unload/reload already loaded classes at runtime to allow
    // changes to Java templates in Clownfish without a restart

    public ArrayList<Class<?>> compileClasses(ArrayList<File> java, boolean withMessage)
    {
        //CfClassLoader cl = (CfClassLoader) ClassLoader.getSystemClassLoader();
        // Map<String, byte[]> classBytes = new HashMap<>();
        ArrayList<Class<?>> newClasses = new ArrayList<>();
        setCompileOut(new StringWriter());

        if (!java.isEmpty()) {
        
            try
            {
                List<String> options = new ArrayList<>(Arrays.asList("-classpath", constructClasspath()));

                File tmpDirRoot = new File(getTmpdir().getParent().getParent().getParent().toString());
                cfclassLoader.add(tmpDirRoot.toURI().toURL());

                options.addAll(Arrays.asList("-d", tmpDirRoot.toString()));

                if (verboseCompile)
                    options.addAll(List.of("-verbose"));

                // URL[] classpath = cl.getURLs();
                // LOGGER.info("Classpath:");
                // for (URL url : classpath)
                // {
                //     options.add(url.getPath());
                //     LOGGER.info(url.toString());
                // }

                JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
                StandardJavaFileManager jfm = javac.getStandardFileManager(null, null, Charset.defaultCharset());

                jfm.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singleton(getTmpdir().toFile()));

                Iterable<? extends JavaFileObject> compilationUnits = jfm.getJavaFileObjectsFromFiles(java);
                JavaCompiler.CompilationTask task = javac.getTask(compileOut, jfm, null, options,
                        Collections.emptySet(), compilationUnits);

                for (JavaFileObject jfo : compilationUnits)
                    LOGGER.info("Compiling " + jfo.getName() + "...");

                if (task.call())
                {
                    // ArrayList<FileObject> fileObjects = new ArrayList<>();

                    for (File file : java)
                    {
                        String className = file.getName().replaceFirst("[.][^.]+$", "");
                        LOGGER.info("COMPILING " + className + "...");
                        // FileObject fo = jfm.getJavaFileForInput(StandardLocation.CLASS_OUTPUT, "", JavaFileObject.Kind.CLASS);
                        // fileObjects.add(fo);
                        // classBytes.put(className, Files.readAllBytes(Paths.get(fo.toUri())));
                        newClasses.add(cfclassLoader.loadClass("io.clownfish.internal." + className));
                    }

                    for (Class<?> clazz : newClasses)
                    {
                        classMethodMap.put(clazz, new ArrayList<>(Arrays.asList(clazz.getDeclaredMethods())));

                        LOGGER.info("Class name: " + clazz.getCanonicalName());
                        LOGGER.info("Class package name: " + clazz.getPackageName());
                        LOGGER.info("Class loader: " + clazz.getClassLoader());

                        classMethodMap.forEach((k, v) -> v.forEach(method -> LOGGER.info(k.getSimpleName() + ": " + method.getName())));
                        // LOGGER.info("Class path: " + clazz.getResource(clazz.getSimpleName() + ".class").toString());
                        // LOGGER.info("Main method invocation:");
                        // newClass.getMethod("main", String[].class).invoke(null, (Object) null);
                    }

                    if (withMessage)
                    {
                        FacesMessage message = new FacesMessage("Compiled class(es) successfully");
                        FacesContext.getCurrentInstance().addMessage(null, message);
                    }
                }
                else
                {
                    if (withMessage)
                    {
                        FacesMessage message = new FacesMessage("Compilation failed! Check compile log.");
                        FacesContext.getCurrentInstance().addMessage(null, message);
                    }
                }
            }
            catch (ClassNotFoundException | IOException | IllegalStateException /*| NoSuchMethodException | IllegalAccessException | InvocationTargetException*/ e)
            {
                LOGGER.error(e.getMessage());
            }

            return newClasses;
        } else {
            return null;
        }
    }

    // Create classpath String from temp dir, maven libs and custom libs for compilation
    public String constructClasspath() throws IOException
    {
        // StringBuilder classPath = new StringBuilder(System.getProperty("java.class.path").trim());
        StringBuilder classPath = new StringBuilder(System.getProperty("java.class.path"));

        // $CLOWNFISH_DIR/cache/src/io/clownfish/internal
        // classPath.append(classpathDelim()).append("\"").append(getTmpdir().toString()).append("\"");
        classPath.append(classpathDelim()).append(getTmpdir().toString());

        // $CLOWNFISH_DIR/maven/
        List<File> maven = Files.walk(Paths.get(propertyUtil.getPropertyValue("folder_maven")))
                        .filter(Files::isRegularFile).sorted(Comparator.reverseOrder()).map(Path::toFile)
                        .collect(Collectors.toList());

        if (!maven.isEmpty())
        {
            maven.stream().filter(file -> file.getName().endsWith(".jar")).collect(Collectors.toList())
                    //.forEach(jar -> classPath.append(classpathDelim()).append("\"").append(jar.getAbsolutePath()).append("\""));
                    .forEach(jar -> classPath.append(classpathDelim()).append(jar.getAbsolutePath()));
        }

        // $CLOWNFISH_DIR/libs
        List<File> libs = Files.walk(Paths.get(propertyUtil.getPropertyValue("folder_libs")))
                .filter(Files::isRegularFile).sorted(Comparator.reverseOrder()).map(Path::toFile)
                .collect(Collectors.toList());

        if (!libs.isEmpty())
        {
            libs.stream().filter(file -> file.getName().endsWith(".jar")).collect(Collectors.toList())
                    //.forEach(jar -> classPath.append(classpathDelim()).append("\"").append(jar.getAbsolutePath()).append("\""));
                    .forEach(jar -> classPath.append(classpathDelim()).append(jar.getAbsolutePath()));
        }
        LOGGER.info("CLASSPATH: " + classPath.toString());
        return classPath.toString();
    }

    public void onCompileAll(ActionEvent actionEvent)
    {
        compileAll(true);

        FacesMessage message = new FacesMessage("Compiled class(es) successfully");
        FacesContext.getCurrentInstance().addMessage(null, message);
    }
    
    public void compileAll(boolean withMessage) {
        if (getTmpdir() == null)
        {
            try
            {
                createTempDir();
            }
            catch (IOException e)
            {
                LOGGER.error(e.getMessage());
            }
        }

        ArrayList<File> javas = new ArrayList<>();

        for (CfJava java : cfjavaService.findAll())
        {
            if (tmpdir != null)
            {
                File src = new File(getTmpdir().toFile() + File.separator + java.getName() + ".java");

                try (Writer srcWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(src), StandardCharsets.UTF_8)))
                {
                    srcWriter.write(java.getContent());
                    javas.add(src);
                }
                catch (IOException e)
                {
                    LOGGER.error(e.getMessage());
                }
            }
        }

        compileClasses(javas, withMessage);
    }

    public void createTempDir() throws IOException
    {
        //CfClassLoader cl = (CfClassLoader) ClassLoader.getSystemClassLoader();
        Path tmp = Files.createTempDirectory(Paths.get(propertyUtil.getPropertyValue("folder_cache")), "src");

        // $CLOWNFISH_DIR/cache/src/io/clownfish/internal
        File dirs = new File(Paths.get(tmp.toString()
                + File.separator + "io"
                + File.separator + "clownfish"
                + File.separator + "internal").toString());
        dirs.mkdirs();
        setTmpdir(dirs.toPath());

        // Add temp dir to classpath
        cfclassLoader.add(dirs.toURI().toURL());

        // Recusively delete temp dir and contents on graceful shutdown
        Thread deleteHook = new Thread(() ->
        {
            try
            {
                Stream<Path> files = Files.walk(tmp);
                files.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::deleteOnExit);
                files.close();

                Stream<Path> directories = Files.walk(tmp.getParent());
                directories.filter(dir -> Files.isDirectory(dir)).map(Path::toFile).forEach(File::deleteOnExit);
                directories.close();
            }
            catch (IOException e)
            {
                LOGGER.error(e.getMessage());
            }
        });
        Runtime.getRuntime().addShutdownHook(deleteHook);
    }

    private boolean isWindows()
    {
        return System.getProperty("os.name").startsWith("Windows");
    }

    private String classpathDelim()
    {
        return isWindows() ? ";" : ":";
    }
}