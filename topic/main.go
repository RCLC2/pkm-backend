package main

import (
	"log"
	"topic/esclient"
	"topic/server"

	"github.com/elastic/go-elasticsearch/v8"
)

func main() {
	cfg := elasticsearch.Config{
		Addresses: []string{"http://elasticsearch:9200"},
	}

	es, err := esclient.NewElasticService(cfg, "pkm-note")
	if err != nil {
		log.Fatalf("Error in esclient: %s", err)
	}

	srv := server.NewServer(es)
	if err := srv.Run(":8082"); err != nil {
		log.Fatalf("Error in starting server: %s", err)
	}
}
