PROMPT Do you want to create the default table for DICOM image?

PAUSE Press OK to create default table.

-- Create tablespace
create tablespace dicomdata datafile 'dicomdata' size 100m 
EXTENT MANAGEMENT LOCAL SEGMENT SPACE MANAGEMENT AUTO;

--  The standard process for creating fully instantiated ORDDicom objects
--  has several steps:
--
--  1. insert an empty initialized ORDDicom object
--  2. load binary DICOM data into the underlying BLOB 
--  3. select ORDDicom object from table for update
--  4. call obj.setProperties() method to extract metadata and populate object attributes
--  5. update table with modified object 
--
--  Step 4 will increase the size of the ORDDicom object. Thus it will require more 
--  space in the database page when it is written back to the table in step 5. Oracle
--  reserves space on every database page to accomodate the grow of data items. The 
--  amount of space is determined by the PCTFREE attribute (default value 10) which 
--  is specified in the CREATE TABLE statement. 
--  
--  To determine a suitable value for PCTFREE perform the following:
--
--  1. create a test table and load it with data
--  2. execute the command "ANALYZE TABLE <table_name> COMPUTE STATISTICS"
--  3. execute the query "SELECT AVG_ROW_LEN, CHAIN_CNT 
--                        FROM USER_TABLES WHERE TABLE_NAME='<table_name>'"
--  4. initialize the ORDDicom objects in the table using the setProperties() method
--  5. perform steps 2 and 3 again.
--  6. compute a value for PCTFREE as 100*(1-X/Y)  where X is the AVG_ROW_LEN 
--     from step 3 and Y is the AVG_ROW_LEN from step 5
--   
--    NOTE this discussion assume that AVG_ROW_LEN < the database block size
--         You should ensure the your AVG_ROW_LEN is less than your database block size
--         so that row chaining does not occur automatically for every row.
--
--  The table definition below uses a value of 60 for the PCTFREE attribute.
--  Note that each LOB attribute of the ORDDicom object has a storage clause that
--  disables in row storage for that LOB data. This increases the predictablity of 
--  row length and reduces row chaining.

create table dicom_image 
(
    i_id  integer primary key ,  -- image id
    i_date date ,
    i_image ordsys.orddicom      -- dicom image
)
--
-- table storage 
--
--  metadata extraction expands the orddicom image considerably
--
    pctfree 60
--
-- lob storage
--
    lob(i_image.source.localdata) store as SecureFile 
  ( tablespace dicomdata
          nocache filesystem_like_logging
      ), 
--     disable in row storage for the extension 
--     so that it does not consume page space
--     it is usually < 4k in size
    lob(i_image.extension) store as SecureFile 
  ( tablespace dicomdata
          nocache filesystem_like_logging
          disable storage in row
      ),
--     store the metadata as a CLOB, 
--     disable storage in row
    xmltype i_image.metadata store as SecureFile clob
	( tablespace dicomdata
          nocache 
          disable storage in row
      )
; 


 


