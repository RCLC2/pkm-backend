# Stage 1:
FROM golang:1.25-alpine AS builder

WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download
COPY . .

RUN CGO_ENABLED=0 GOOS=linux go build -o /pkm-backend main.go

# Stage 2:
FROM alpine:latest

WORKDIR /root/
COPY --from=builder /pkm-backend .
EXPOSE 8082

CMD ["./pkm-backend"]
