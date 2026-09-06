# Dockerfile для обычного проекта
# ---------- Stage 1: Build ----------
FROM gradle:9.4.1-jdk25-alpine AS builder

WORKDIR /app

# копируем только файлы для кеширования зависимостей
COPY build.gradle settings.gradle  ./
COPY gradle ./gradle

RUN gradle --no-daemon dependencies

# копируем исходники
COPY src ./src

# собираем jar
RUN gradle clean bootJar --no-daemon


# ---------- Stage 2: Runtime ----------
FROM eclipse-temurin:25-jdk-jammy

WORKDIR /app

# копируем jar из стадии сборки
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]

# Dockerfile для монорепозитория
## ---------- Stage 1: Build ----------
#FROM gradle:9.4.1-jdk25-alpine AS builder
#WORKDIR /app
#
## Копируем корневые файлы конфигурации
#COPY build.gradle settings.gradle ./
#COPY gradle ./gradle
#
## Копируем build.gradle сервиса
#COPY service_folder_name/build.gradle service_folder_name/
#
## Сначала резолвим зависимости всего проекта
#RUN gradle --no-daemon dependencies
#
## копируем исходники только нужного сервиса
#COPY service_folder_name/src ./service_folder_name/src
#
## Собираем только нужный subproject (bootJar)
#RUN gradle clean :service_folder_name:bootJar --no-daemon
#
## ---------- Stage 2: Runtime ----------
#FROM eclipse-temurin:25-jdk-jammy
#WORKDIR /app
#
## Копируем собранный jar из subproject
#COPY --from=builder /app/service_folder_name/build/libs/*.jar app.jar
#
#EXPOSE 7771
#ENTRYPOINT ["java", "-jar", "app.jar"]
