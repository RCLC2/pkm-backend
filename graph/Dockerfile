# Stage 1:
FROM golang:1.25-alpine AS builder

WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download

COPY . .
RUN CGO_ENABLED=0 GOOS=linux go build -o graph main.go

# Stage 2:
FROM alpine:latest

WORKDIR /root/
COPY --from=builder /app/graph .
EXPOSE 8082

CMD ["./graph"]