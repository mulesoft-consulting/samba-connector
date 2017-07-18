# samba-connector

This Connector can be usd by any MuleSoft client unlike the one at https://github.com/mulesoft-consulting/mule-smb-connector.
Please let me (aminul.haque@mulesoft.com) know if you would like any new processors (features) to be added on this connector.

## Following are the current capabilities of this connector:
- **saveFileFromStream:** This method will save a file in a file location.
- **readFiles:** This processor will return a map of fully qualified file name and its content present at theconfigured folder based on a file name pattern
- **readFileNames:** This processor will return a list of fully qualified file name present at the   configured folder based on a file name pattern
- **readFile:** Message processor that can be directly called to read a specified File	
- **deleteFile:** This processor will delete any file specified by the file name

## How to use

### Below is sample Samba configuration:
  ```xml
  <smb:config name="Smb__Configuration" username="USER" password="XXXX" host="host" domain="WORKGROUP" folder="shared$/AMINUL" doc:name="Smb: Configuration"/>
  ```
  
  
### Then in a flow use the endpoint as below:
  ```xml
  <smb:read-file config-ref="Smb__Configuration" fileName="settings.xml" doc:name="Smb"/>
  ```
*settings.xml being a sample file i am trying to read*
