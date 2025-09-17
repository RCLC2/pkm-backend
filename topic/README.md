
## Quick Started

- start: `docker-compose up --build`
- swagger docs: `swag init`
  - url: http://localhost:8082/swagger/index.html
- nori install: `/usr/share/elasticsearch/bin/elasticsearch-plugin install analysis-nori`

# Start to ElasticSearch

nori_analyzer & content mapping

```
curl -X PUT "http://localhost:9200/pkm-note" -H "Content-Type: application/json" -d'
{
  "settings": {
    "index": {
      "analysis": {
        "analyzer": {
          "nori_analyzer": {
            "type": "custom",
            "tokenizer": "nori_tokenizer"
          }
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "content": {
        "type": "text",
        "analyzer": "nori_analyzer"
      }
    }
  }
}'
```