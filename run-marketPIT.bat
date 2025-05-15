@echo on
SET MARKET_DIR=%~dp0%
SET MARKETCORE_DIR=%~dp0%/market-core
cd %MARKET_DIR%
cmd /c mvn package -DskipTests=true
cd %MARKETCORE_DIR%
cmd /c mvn test-compile org.pitest:pitest-maven:mutationCoverage
pause