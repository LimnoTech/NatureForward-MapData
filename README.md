# NatureForward-MapData
This repository is for Nature Forward to convert their Salesforce data into usable data for ArcGis.


**Old Map**: https://natureforward.my.salesforce-sites.com/enterWQMSession/displaymapmostrecentwqm

<br />
<br />

Quick Links:  
[Updating Sites Feature Layer](#1-updating-sites-featurelayer)  
[Updating Critters Table](#2-updating-critters-table)  
[Updating IBI Scores Table](#3-updating-ibi-scores-table)

# 1. Updating Sites FeatureLayer
## 1.1 Download `LimnoTech Sites` Report
Navigate to the [`Reports`](https://natureforward.my.salesforce.com/00O/o) tab on Salesforce. Under the **LimnoTech** folder there is a report titled [`LimnoTech Sites`](https://natureforward.my.salesforce.com/00OUw000001pS05).

Click `Run Report` and then click `Export Details`.  
![alt text](images/report_buttons.png)   

Change to the following export settings and then click `Export`.
- **Export File Encoding** = `Unicode (UTF-8)` 
- **Export File Format** = `Comma Delimited .csv`  

![alt text](images/export_page.png) 

Your file will then download to your browser. The file name will be similar to ***report1719589800854.csv***, but with different numbers.

## 1.2 Run the ArcGIS Online Jupyter Notebook
Under the [Notebook](https://anshome.maps.arcgis.com/home/notebook/notebookhome.html) tab in ArcGIS online, open the [Update Sites FeatureLayer]() notebook.

Click on the cell with code and then click `Run`. While the cell is running, it will have this symbol `In [*]`, once finished it will change to `In [1]`.  
![alt text](images/run_notebook.png)   
 
Scroll to the bottom of the notebook and click the `Upload` button. Upload the report you downloaded in [Step 1](#11-download-limnotech-sites-report), then click `Update Layer`.  
![alt text](images/sites_upload.png)    

A progress bar will appear and update while the code is running. The layer is successfully updated when the progress bar reaches 100% and `Sites layer updated` is displayed.  
![alt text](images/sites_updated.png)  

# 2. Updating Critters Table

## 2.1 Download `LimnoTech Session/Critter` Report
Navigate to the [`Reports`](https://natureforward.my.salesforce.com/00O/o) tab on Salesforce. Under the **LimnoTech** folder there is a report titled [`LimnoTech Session/Critter`](https://natureforward.my.salesforce.com/00OUw000001htqH).

In the `Time Frame` box, change the `From` date to 3 years prior (ex: 1/1/2022 -> 1/1/2019). This change will capture the last 5 years of critter data.  
![alt text](images/date_range.png)  

Click `Run Report` and then click `Export Details`.   
![alt text](images/report_buttons.png)  

Change to the following export settings and then click `Export`.
- **Export File Encoding** = `Unicode (UTF-8)` 
- **Export File Format** = `Comma Delimited .csv` 

![alt text](images/export_page.png) 

Your file will then download to your browser. The file name will be similar to ***report1719589800854.csv***, but with different numbers.

## 2.2 Run the ArcGIS Online Jupyter Notebook
Under the [Notebook](https://anshome.maps.arcgis.com/home/notebook/notebookhome.html) tab in ArcGIS online, open the [Update Critters Table](https://anshome.maps.arcgis.com/home/notebook/notebook.html?id=c49b75f26cd945b4bcf5e3faa6f7e858) notebook.

Click on the cell with code and then click `Run`. While the cell is running, it will have this symbol `In [*]`, once finished it will change to `In [1]`.   
![alt text](images/run_notebook.png)   
  
Scroll to the bottom of the notebook and click the `Upload` button. Upload the report you downloaded in [Step 1](#11-download-limnotech-sites-report), then click `Update Table`.  
![alt text](images/critters_upload.png)

A progress bar will appear and update while the code is running. The layer is successfully updated when the progress bar reaches 100% and `Critters table updated` is displayed.  
![alt text](images/critters_updated.png)  

# 3. Updating IBI Scores Table
## 3.1. Download `LimnoTech IBI Over Time` Report
Navigate back to the [`Reports`](https://natureforward.my.salesforce.com/00O/o) tab on Salesforce. Under the **LimnoTech** folder there is a report titled [`LimnoTech IBI Over Time`](https://natureforward.my.salesforce.com/00OUw000001huj7).

Click `Run Report` and then click `Export Details`.  
![alt text](images/report_buttons.png)   

Change to the following export settings and then click `Export`.
- **Export File Encoding** = `Unicode (UTF-8)` 
- **Export File Format** = `Comma Delimited .csv`  

![alt text](images/export_page.png) 

Your file will then download to your browser. The file name will be similar to ***report1719589800854.csv***, but with different numbers.

## 3.2 Run the ArcGIS Online Jupyter Notebook
Under the [Notebook](https://anshome.maps.arcgis.com/home/notebook/notebookhome.html) tab in ArcGIS online, open the [Update IBI Scores Table](https://anshome.maps.arcgis.com/home/notebook/notebook.html?id=bb8de15473674581ada5d5ed64b019d5) notebook.

Click on the cell with code and then click `Run`. While the cell is running, it will have this symbol `In [*]`, once finished it will change to `In [1]`.   
![alt text](images/run_notebook.png)   

Scroll to the bottom of the notebook and click the `Upload` button. Upload the report you downloaded in [Step 1](#11-download-limnotech-sites-report), then click `Update Table`.   
![alt text](images/ibi_upload.png)  

A progress bar will appear and update while the code is running. The layer is successfully updated when the progress bar reaches 100% and `IBI Scores table updated` is displayed.  
![alt text](images/ibi_updated.png)  

