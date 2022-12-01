FROM eclipse-temurin:19-jre-alpine
EXPOSE 8080:8080
# run as non-root user
RUN addgroup -g 1001 -S user && adduser -u 1001 -S -s /bin/false -G user user
USER user
COPY ./build/install/org.eclipse.tractusx.managedidentitywallets/ /app/
COPY ./static /app/static
WORKDIR /app/bin
CMD ["./org.eclipse.tractusx.managedidentitywallets"]
