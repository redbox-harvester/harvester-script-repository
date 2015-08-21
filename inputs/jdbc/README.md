## JDBC configuration

* In harvester-config groovy file, in 'harvest' key, place (examples of attribute values provided):
```
jdbc {
        user = 'SA'
        pw = ''
        driver = 'org.hsqldb.jdbcDriver'
        url = 'jdbc:hsqldb:file:db/data/local'
        Dataset {
            query = 'SELECT * FROM "dataset" WHERE "last_updated" >= TIMESTAMP(:last_harvest_ts)'
            sqlParam.last_harvest_ts = '2015-06-05 13:08:25'
        }
    }
```