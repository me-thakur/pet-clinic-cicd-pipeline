#!/bin/bash

echo "=== Pet Clinic Test Diagnostics ==="
echo "Date: $(date)"
echo

echo "=== Environment Information ==="
echo "Java Version:"
java -version
echo
echo "Maven Version:"
mvn -version
echo

echo "=== Checking for jqwik remnants ==="
echo "Searching for jqwik in pom.xml files:"
find . -name "pom.xml" -exec grep -l "jqwik" {} \; 2>/dev/null || echo "No jqwik found in pom.xml files ✓"
echo

echo "Searching for jqwik imports in Java files:"
find . -name "*.java" -exec grep -l "import.*jqwik" {} \; 2>/dev/null || echo "No jqwik imports found ✓"
echo

echo "Searching for .jqwik-database files:"
find . -name ".jqwik-database" 2>/dev/null || echo "No .jqwik-database files found ✓"
echo

echo "=== Maven Dependency Tree (jqwik check) ==="
cd pet-clinic-backend
echo "Checking backend dependencies for jqwik:"
mvn dependency:tree | grep -i jqwik || echo "No jqwik dependencies found ✓"
echo

echo "=== Running Clean Test with Verbose Output ==="
echo "Running: mvn clean test -X"
echo "This will show detailed Maven debug output..."
echo