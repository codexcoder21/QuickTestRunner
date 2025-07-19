# QuickTestRunner

QuickTestRunner is a simple tool for executing small test files called `quicktest.kts`.

A `quicktest.kts` file contains top level Kotlin functions. Each function is treated as an
individual unit test. Files are compiled using the embedded Kotlin compiler and executed
within the same JVM – the `.kts` extension is only a name and no Kotlin scripting
frameworks are used.

## Usage

```
./gradlew run --args='--directory path/to/search'
```

The program recursively searches the provided directory for every `quicktest.kts` file,
compiles them and runs all top level functions. A test passes if it completes without
throwing an exception.

## Building and testing

Run the following command to build the project and execute unit tests:

```
./gradlew test
```
