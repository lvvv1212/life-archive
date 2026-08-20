# ====== Stage 1: 编译（用 Maven + JDK 22 在容器内编译源码）======
FROM maven:3.9-eclipse-temurin-22 AS builder

WORKDIR /build

# 配置国内 Maven 镜像（阿里云），避免容器内访问中央仓库超时导致依赖下载失败
RUN mkdir -p /root/.m2 && \
    printf '%s\n' \
    '<settings>' \
    '  <mirrors>' \
    '    <mirror>' \
    '      <id>aliyun-public</id>' \
    '      <name>Aliyun Maven</name>' \
    '      <url>https://maven.aliyun.com/repository/public</url>' \
    '      <mirrorOf>central</mirrorOf>' \
    '    </mirror>' \
    '  </mirrors>' \
    '</settings>' \
    > /root/.m2/settings.xml

# 先拷 pom.xml 下载依赖（利用 Docker 层缓存，改代码不用重下依赖）
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 拷贝源码并编译
COPY src ./src
RUN mvn package -DskipTests -B

# ====== Stage 2: 运行（轻量 JRE 镜像）======
FROM eclipse-temurin:22-jre

WORKDIR /app
RUN mkdir -p upload

COPY --from=builder /build/target/life-archive-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
