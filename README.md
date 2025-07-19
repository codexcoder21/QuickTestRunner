# QuickTestRunner

QuickTestRunner is a simple tool for executing small test files called `quicktest.kts`.

A `quicktest.kts` file contains top level Kotlin functions. Each function is treated as an
individual unit test. Files are compiled using the embedded Kotlin compiler and executed
within the same JVM – the `.kts` extension is only a name and no Kotlin scripting
frameworks are used.

## Usage

```
./gradlew run --args='--directory path/to/search --log results.xml'
```

If the log file ends with `.xml` an XML report is created. If it ends with `.html`,
the report will be an HTML file.

The program recursively searches the provided directory for every `quicktest.kts` file,
compiles them and runs all top level functions. A test passes if it completes without
throwing an exception.

### Programmatic usage

You can also run tests directly from Kotlin code using `QuickTestRunner`:

```
val results = QuickTestRunner()
    .directory(File("path/to/tests"))
    .logFile(File("results.xml"))
    .run()
```

`QuickTestRunResults` provides access to the individual `TestResult` entries.

## Building and testing

Run the following command to build the project and execute unit tests:

```
./gradlew test
```
