LOAD DATA
infile 'D:\Sid-SparkScala\workspace\datapipeline_demo\src\main\resources\TransactionSync.csv'
into table HR.TRANSACTIONSYNC
fields terminated by ',' OPTIONALLY ENCLOSED BY '"'
TRAILING NULLCOLS
(App_Transaction_Id,
Internal_Member_Id,
Location_External_Reference,
Transaction_Type_Id,
Transaction_Timestamp,
Activity_Timestamp,
Activity_App_Date,
Transaction_Retail_Value,
Transaction_Profit_Value,
Transaction_Base_Point_Value,
Transaction_Point_Value,
Transaction_Won_Value,
Event_Name,
Issue_Audit_User,
Transaction_External_Reference,
Cancel_Timestamp,
Cancel_Audit_User
)