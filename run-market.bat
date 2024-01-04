@echo on
SET MARKET_DIR=%~dp0%
cd %MARKET_DIR%
cmd /c mvn package -DskipTests=true
docker build -t market-web --build-arg module=market-web .
docker build -t market-rest --build-arg module=market-rest .
docker-compose -f docker-compose.yaml down
docker volume rm market-main-fork_postgres-data
docker-compose -f docker-compose.yaml up -d
pause