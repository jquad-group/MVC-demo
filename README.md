# RefSys Aggregation Query Service

## API Generation from batch file
To generate the APIs for ACDS and Aggregation Service the "scripts/generate_acds_sources.bat" file needs to be run. This will unpack and generate
the source files and run mvn clean and mvn compile.

## Unittests
The tests are using a flapdoodle (embedded mongo) instance. This instance is copied to the local machine on first testrun.
The config (EmbeddedMongoDbConfiguration) tries to figure out if the internal repo for this is reachable and if not (on CNC for example) it will try to use the internet.

## Local Run Requirments

### Run Kafka locally

Download [kafka](https://www.apache.org/dyn/closer.cgi?path=/kafka/3.6.1/kafka_2.13-3.6.1.tgz)
and extract it in your user home directory as kafka folder. If you want to use a different folder, you can do so, simply adjust the variables in the settings.bat
(located in the script directory)

* run the script scripts/start-kafka.bat in a Command Prompt. -> this script opens a terminal window for zookeper & kafka-server.
* if you would like to use the Services Window of IntelliJ for this purpose you could start the commands separately:
    * scripts/start_zookeeper.bat
    * scripts/start_server.bat
    * scripts/create_topics.bat

### Mongodb Install

Download [mongodb](https://fastdl.mongodb.org/windows/mongodb-windows-x86_64-7.0.5.zip)
and [mongosh](https://downloads.mongodb.com/compass/mongosh-2.1.1-win32-x64.zip) as zip
and extract them in your user home directory as mongodb and mongosh folders.
If you want to use a different folder, you can do so, simply adjust the variables in the settings.bat.

* run the script scripts/start-mongodb.bat in a Command Prompt.

Notes:

The following step must be executed after running the db, and only for one time for a clean data:

* Initiate the replica set cluster using mongosh app in Mongosh_folder/bin. run: `rs.initiate()`

To erase and clean the data, delete the data folder in Mongodb_folder

### Avoid double BDD tests execution locally in intellij

* The default junit test run configuration will execute everything in the java directory including the bdd features in resources.
* Sametime it will execute the RunCucumberTest.java which is required for maven.

**Solution**:

create junit test run configuration which will run tests only in de.datev.refsys package, this will take only the java tests.

![junit-test-run-config.png](src%2Ftest%2Fresources%2Fjunit-test-run-config.png)



# Indexes händisch anlegen

```
in Mongo Compass zuerst

"use refsys_aggregation" ausführen 

## Indexes 

db.masterData.createIndex( { "consultant" : 1 , "client" : 1 , "year_begin" : 1 , "year_end" : 1 },{"unique" :true} )

db.masterDataAccounts.createIndex( { "consultant" : 1 , "client" : 1 , "used" : 1, "year_begin" : 1 , "year_end" : 1 }, { partialFilterExpression: { "used": true } } )

db.masterDataAccounts.createIndex( { "consultant" : 1 , "client" : 1 , "year_begin" : 1 , "year_end" : 1 , "account_number_from": 1, "account_number_to": 1 },{"unique" :true} )

db.stateDoc.createIndex( { "consultant" : 1 , "client" : 1 , "year_begin" : 1 , "year_end" : 1 },{"unique" :true} )

db.movementDataDays.createIndex( { "consultant" : 1 , "client" : 1 ,  "fiscal_year" : 1 , "account_number" : -1 , "accounting_reason_id" : 1 , "additional_params" : 1  },{"unique" :true} )

db.movementDataMonths.createIndex( { "consultant" : 1 , "client" : 1 , "fiscal_year" : 1 , "account_number" : -1 , "accounting_reason_id" : 1 , "additional_params" : 1   },{"unique" :true} )

db.movementDataPersonGroupDays.createIndex( { "consultant" : 1 , "client" : 1 , "fiscal_year" : 1 , "account_group_number" : -1 , "accounting_reason_id" : 1 , "additional_params" : 1   },{"unique" :true} )

db.movementDataPersonGroupMonths.createIndex( { "consultant" : 1 , "client" : 1 , "fiscal_year" : 1 , "account_group_number" : -1 , "accounting_reason_id" : 1 , "additional_params" : 1   },{"unique" :true} )

db.movementDataInventories.createIndex( { "consultant" : 1 , "client" : 1 ,  "fiscal_year" : 1 , "account_number" : -1 , "anlag_accounting_reason" : 1  },{"unique" :true} )

db.customColumnStructureContents.createIndex( { "consultant" : 1 , "client" : 1 ,  "year_begin" : 1 , "year_end" : 1 , "column_structure_id" : 1 , "industry_no" : 1 , "section_no" : 1 , "indiv_no" : 1 , "national_right" : 1 , "indiv_level" : 1   },{"unique" :true} )

db.customReportStructureContents.createIndex( { "consultant" : 1 , "client" : 1 ,  "year_begin" : 1 , "year_end" : 1 , "report_structure_id" : 1 , "industry_no" : 1 , "section_no" : 1 , "indiv_no" : 1 , "national_right" : 1 , "indiv_level" : 1   },{"unique" :true} )

## Sharding

 db.adminCommand(
   {
     shardCollection: "refsys_aggregation.masterData",
     key: { "consultant": 1, "client":  1,  "year_begin": 1}
   }
 )
 
  db.adminCommand(
   {
     shardCollection: "refsys_aggregation.masterDataAccounts",
     key: { "consultant": 1, "client":  1,  "year_begin": 1}
   }
 )
 
db.adminCommand(
   {
     shardCollection: "refsys_aggregation.stateDoc",
     key: { "consultant": 1, "client":  1,  "year_begin": 1}
   }
 )
 
db.adminCommand(
   {
     shardCollection: "refsys_aggregation.movementDataDays",
     key: { "consultant": 1, "client":  1,  "fiscal_year": 1}
   }
 )
 
db.adminCommand(
   {
     shardCollection: "refsys_aggregation.movementDataMonths",
     key: { "consultant": 1, "client":  1,  "fiscal_year": 1}
   }
 ) 

db.adminCommand(
   {
     shardCollection: "refsys_aggregation.movementDataPersonGroupDays",
     key: { "consultant": 1, "client":  1,  "fiscal_year": 1}
   }
 )  
 
db.adminCommand(
   {
     shardCollection: "refsys_aggregation.movementDataPersonGroupMonths",
     key: { "consultant": 1, "client":  1,  "fiscal_year": 1}
   }
 )
 
db.adminCommand(
   {
     shardCollection: "refsys_aggregation.movementDataInventories",
     key: { "consultant": 1, "client":  1,  "fiscal_year": 1}
   }
 )
 
db.adminCommand(
   {
     shardCollection: "refsys_aggregation.customColumnStructureContents",
     key: { "consultant": 1, "client":  1,  "year_begin": 1}
   }
 )
 
db.adminCommand(
   {
     shardCollection: "refsys_aggregation.customReportStructureContents",
     key: { "consultant": 1, "client":  1,  "year_begin": 1}
   }
 )
 
 ## Cleanup
 
db.masterData.deleteMany({})

db.masterDataAccounts.deleteMany({})

db.masterDataAccounts.deleteMany({})

db.stateDoc.deleteMany({})

db.movementDataDays.deleteMany({})

db.movementDataMonths.deleteMany({})

db.movementDataPersonGroupDays.deleteMany({})

db.movementDataPersonGroupMonths.deleteMany({})

db.movementDataInventories.deleteMany({})

db.customColumnStructureContents.deleteMany({})

db.customReportStructureContents.deleteMany({})
 
```

