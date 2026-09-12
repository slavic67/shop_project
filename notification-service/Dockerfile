# Dockerfile для обычного проекта
## ---------- Stage 1: Build ----------
#FROM gradle:9.4.1-jdk25-alpine AS builder
#
#WORKDIR /app
#
## копируем только файлы для кеширования зависимостей
#COPY build.gradle settings.gradle  ./
#COPY gradle ./gradle
#
#RUN gradle --no-daemon dependencies
#
## копируем исходники
#COPY src ./src
#
## собираем jar
#RUN gradle clean bootJar --no-daemon
#
#
## ---------- Stage 2: Runtime ----------
#FROM eclipse-temurin:25-jdk-jammy
#
#WORKDIR /app
#
## копируем jar из стадии сборки
#COPY --from=builder /app/build/libs/*.jar app.jar
#
#EXPOSE 8081
#
#ENTRYPOINT ["java", "-jar", "app.jar"]

# Dockerfile для монорепозитория
# ---------- Stage 1: Build ----------
FROM gradle:9.4.1-jdk25-alpine AS builder
WORKDIR /app

# Копируем корневые файлы конфигурации
COPY build.gradle ./notification-service/settings.gradle ./
COPY gradle ./gradle

# Копируем build.gradle сервиса
COPY notification-service/build.gradle notification-service/

# Сначала резолвим зависимости всего проекта
RUN gradle --no-daemon dependencies

# копируем исходники только нужного сервиса
COPY notification-service/src ./notification-service/src

# Собираем только нужный subproject (bootJar)
RUN gradle clean :notification-service:bootJar --no-daemon

# ---------- Stage 2: Runtime ----------
FROM eclipse-temurin:25-jdk-jammy
WORKDIR /app

# Копируем собранный jar из subproject
COPY --from=builder /app/notification-service/build/libs/*.jar app.jar

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
