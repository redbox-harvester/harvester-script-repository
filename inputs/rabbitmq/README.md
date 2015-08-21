## RabbitMQ configuration

* In harvester-config groovy file, in 'harvest' key, place (examples of attribute values provided):
```
 rabbitmq {
        url = '127.0.0.1'
        username = 'demo'
        password = 'demo'
        queuename = 'arms-redbox'
        port = 5672
    }
```