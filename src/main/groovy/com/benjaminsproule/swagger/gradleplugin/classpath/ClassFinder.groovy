package com.benjaminsproule.swagger.gradleplugin.classpath

import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetOutput
import org.reflections.Reflections
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.lang.annotation.Annotation

class ClassFinder {
    private static final Logger LOG = LoggerFactory.getLogger(ClassFinder)
    static instance
    Map<Class<? extends Annotation>, Set<Class<?>>> classCache
    Project project

    private ClassFinder(Project project) {
        this.project = project
        this.classCache = new HashMap<>()
    }

    //FIXME hack until we have some DI working
    static void createInstance(Project project) {
        instance = new ClassFinder(project)
    }

    static ClassFinder instance() {
        return instance
    }

    Set<Class<?>> getValidClasses(Class<? extends Annotation> clazz) {
        return getValidClasses(clazz, [])
    }

    Set<Class<?>> getValidClasses(Class<? extends Annotation> clazz, List<String> packages) {
        if (classCache.containsKey(clazz)) {
            return classCache.get(clazz)
        }

        Set<Class<?>> classes = new HashSet<Class<?>>()
        ClassLoader classLoader = prepareClassLoader()

        if (packages) {
            packages.each { location ->
                Set<Class<?>> c = new Reflections(classLoader, location).getTypesAnnotatedWith(clazz)
                classes.addAll(c)
            }
        } else {
            LOG.warn("Scanning the the entire classpath (${clazz}), you should avoid this by specifying package locations")
            Set<Class<?>> c = new Reflections(classLoader, '').getTypesAnnotatedWith(clazz)
            classes.addAll(c)
        }

        classCache.put(clazz, classes)
        return classes
    }

    private ClassLoader prepareClassLoader() {
        def urls = []
        project.configurations.runtime.resolve().each {
            urls.add(it.toURI().toURL())
        }

        if (SourceSetOutput.class.getFields().contains('classesDirs')) {
            project.sourceSets.main.output.classesDirs.each {
                urls.add(it.toURI().toURL())
            }
        } else {
            urls.add(project.sourceSets.main.output.classesDir.toURI().toURL())
        }

        return new URLClassLoader(urls as URL[], getClass().getClassLoader())
    }
}
