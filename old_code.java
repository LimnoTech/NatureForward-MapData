/**
 * If you change bg_meter image, please update its width in getLeftOffset() in this class.
 * Baird simplifies date offset.  Now if year==null the query goes back 8 quarters for both WQM and Creek Critters.
 */
global class displayMapController {

    public static final String colorPoor = '#d20400';
    public static final String colorFair = '#eaac00';
    public static final String colorGood = '#606400';
    public static final String colorExcellent = '#006400';

    public Coordinates center { get; set; }

    public Integer earliestYear { get; set; }
    public Decimal zoomLevel { get; set; }
    public string recId { get; set; }
    private static String shr;
    private static Map<String, Critters__c> critters;
    public Boolean SHRDeleted {get; set;}
    public List<CritterSurveyFromMobile__c> lstCritters;

    public String selRecordId { get; set; }

    public static Boolean HasWQMSession {
        get {
            Try {
                Schema.DescribeSObjectResult[] descResult =
                        Schema.describeSObjects(new String[]{
                                'WQMSession__c'
                        });
                system.debug('DescribeSObjectResult is ' + descResult);
                return true;
            } Catch (InvalidParameterValueException e) {
                system.debug('No table named WQMSession__c');
                return false;
            }
        }
        set;
}

    public String Address { get; set; }
    //https://na88.salesforce.com/_ui/common/apex/debug/ApexCSIPage#  CAN I DELETE THIS?

    public displayMapController() {
        String recId = ApexPages.currentPage().getParameters().get('id');
        // Find the Coordinates for the center of the map
        if (String.isNotBlank(recId)) {
                lstCritters = [Select Id, SurveyLat__c, SurveyLng__c from CritterSurveyFromMobile__c where ID = :recId];
            if (lstCritters.size()==0) {
                SHRDeleted = true;
                system.debug('At line 49 I found that the CSFMId did not match any records, so I set SHRDeleted to true');
                return;
            } else {
                // If recId is populated there's just one survey, center the map on the survey
                if (lstCritters.size() == 1) {
                    CritterSurveyFromMobile__c C = lstCritters.get(0);
                    selRecordId = C.Id;
                    center = new Coordinates(C.SurveyLat__c, C.SurveyLng__c);
                } else {
                    // Check to see if recid in URL came from WQM_Session
                    List<WQMSession__c> lstSessions = [
                            Select Id, Site__c, Site__r.LongLat__Latitude__s, Site__r.LongLat__Longitude__s
                            FROM WQMSession__c
                            WHERE Id = :recId
                    ];
                    if (lstSessions != null && lstSessions.size() == 1) {
                        WQMSession__c W = lstSessions.get(0);
                        selRecordId = W.Id;
                        center = new Coordinates(W.Site__r.LongLat__Longitude__s, W.Site__r.LongLat__Latitude__s);

                    }
                }
                zoomLevel = 16;
            }
        } else {
            Organization o = [SELECT Id, Address, Street, City, PostalCode, State, Country FROM Organization];

            if (o.Street != null)
                zoomLevel = 12;
            if (o.City != null)
                zoomLevel = 12; else if (o.State != null)
                zoomLevel = 10; else
                    zoomLevel = 5;

            Address = formatAddress(Address, o.Street);
            Address = formatAddress(Address, o.City);
            Address = formatAddress(Address, o.State);
            Address = formatAddress(Address, o.PostalCode);
            Address = formatAddress(Address, o.Country);
        }
        // query the earliest record with date
        List<WQMSession__c> records = [Select Mon_Date_Time__c FROM WQMSession__c WHERE Season__c = '1-SPRING' AND Mon_Date_Time__c != null ORDER BY Mon_Date_Time__c ASC LIMIT 1];
        if (records != null && records.size() > 0) {
            earliestYear = records.get(0).Mon_Date_Time__c.year();
        } else {
            earliestYear = 1990;
        }
        }

    public static String formatAddress(string address, string part) {
        if (address == null)
            address = '';

        if (String.isNotBlank(part)) {
            if (address != '')
                address += ', ';

            address += part;
        }

        return address;
    }


    @RemoteAction
    public static MarkerProperty[] getMappingData(String yearString, Id selectedId) {
        Integer displayYear;
        if (string.ISNOTBLANK(yearString)) {
            if (yearString.isNumeric() && integer.valueOf(yearString) > 1993) {
                displayYear = integer.valueOf(yearString);
            }
        } else {
            yearString = 'show2Years';
            displayYear = null;
        }
        critters = new Map<String, Critters__c>();

        shr = getResourceURL('SHR');
        Critters__c[] critterList = [SELECT CCName__c, Common_Name__c FROM Critters__c WHERE CCName__c != null];
        List<MarkerProperty> lstMarkers = new list<MarkerProperty>();
        Map<Id, Boolean> sites = new Map<Id, Boolean>();

        system.debug('Before querying for MarkerProperties, yearString is ' + yearString);
        if (displayYear != null) {
            for (CritterSurveyFromMobile__c C : [
                    Select ID, Name, BirthYear__c, CritterIDs__c, Email__c, SensitivityScore__c, Site__c, SurveyDateTime__c,
                            SurveyDateTimeStr__c, SurveyLat__c, SurveyLng__c, SurveyorFirstName__c, SurveyorLastName__c, SurveyorName__c,
                            Team__c, Number_in_Group__c, UserFeedback__c
                    from CritterSurveyFromMobile__c
                    WHERE
                    (SensitivityScore__c != null AND SensitivityScore__c > 0 ) AND
                    (((Id = :selectedId OR CALENDAR_YEAR(SurveyDateTime__c) = :displayYear)
                    AND Approved_for_Map__c = true)
                    OR
                    SurveyDateTime__c = TODAY)
                    LIMIT 500
            ]) {
                lstMarkers.add(new MarkerProperty(C, critterList));
                //}
            }
            // If this is sent from a mobile device with a recId, do not show WQMs
            if (HasWQMSession) {
                for (WQMSession__c W : [
                        Select Id, Site__c, Site__r.Name, Site__r.Station_Nr__c, Site__r.LongLat__Latitude__s,
                                Site__r.LongLat__Longitude__s, Season__c, Avg_IBI_Score__c, (Select Id, Critters__r.Name, Count__c From Session_Organisms__r),
                                Mon_Date_Time__c
                        FROM WQMSession__c
                        WHERE Season__c = '1-SPRING' AND (CALENDAR_YEAR(Mon_Date_Time__c) = :displayYear)
                        ORDER BY Mon_Date_Time__c DESC
                        LIMIT 500
                ]) {
                    if (!sites.containsKey(W.Site__c)) {
                        lstMarkers.add(new MarkerProperty(W));
                        sites.put(W.Site__c, true);
                    }
                }
            }
        }

        else {
            //If year==null, then search for the Creek Critters data for the last two years UNLESS the URL specifies Id
            if (String.isNotBlank(selectedId)) {
                CritterSurveyFromMobile__c CSFM = [
                        SELECT ID, Name, BirthYear__c, CritterIDs__c, Email__c, SensitivityScore__c, Site__c, SurveyDateTime__c,
                                SurveyDateTimeStr__c, SurveyLat__c, SurveyLng__c, SurveyorFirstName__c, SurveyorLastName__c, SurveyorName__c,
                                Team__c, Number_in_Group__c, UserFeedback__c
                        from CritterSurveyFromMobile__c
                        WHERE Id = :selectedId
                ];
                lstMarkers.add(new MarkerProperty(CSFM, CritterList));
            } else {
                for (CritterSurveyFromMobile__c C : [
                        Select ID, Name, BirthYear__c, CritterIDs__c, Email__c, SensitivityScore__c, Site__c, SurveyDateTime__c,
                                SurveyDateTimeStr__c, SurveyLat__c, SurveyLng__c, SurveyorFirstName__c, SurveyorLastName__c, SurveyorName__c,
                                Team__c, Number_in_Group__c, UserFeedback__c
                        from CritterSurveyFromMobile__c
                        WHERE (SensitivityScore__c != null AND SensitivityScore__c > 0 ) AND
                        (((Id = :selectedId OR SurveyDateTime__c >= LAST_N_DAYS:730)
                        AND Approved_for_Map__c = true)
                        OR
                        SurveyDateTime__c = TODAY)
                        LIMIT 500
                ]) {

                    lstMarkers.add(new MarkerProperty(C, critterList));

                }
            }
            // Do not show WQM sessions if a report Id is specified
            if (selectedId == null) {
                if (HasWQMSession) {
                    for (WQMSession__c W : [
                            Select Id, Site__c, Site__r.Name, Site__r.Station_Nr__c, Site__r.LongLat__Latitude__s,
                                    Site__r.LongLat__Longitude__s, Season__c, Avg_IBI_Score__c, (
                                    Select Id, Critters__r.Name, Count__c
                                    From
                                            Session_Organisms__r
                            ), Mon_Date_Time__c
                            FROM WQMSession__c
                            WHERE Season__c = '1-SPRING' AND
                            Mon_Date_Time__c >= LAST_N_DAYS:730
                            ORDER BY Mon_Date_Time__c DESC
                            LIMIT 500
                    ]) {
                        if (!sites.containsKey(W.Site__c)) {
                            lstMarkers.add(new MarkerProperty(W));
                            sites.put(W.Site__c, true);
                            system.debug('Added to lstMarkers this WQMSession: ' + W);
                        }
                    }
                }
            }
        }


        return lstMarkers;
    }


    @RemoteAction
    public static MarkerProperty[] getTimelapseData(Integer startYear, Integer endYear) {
        critters = new Map<String, Critters__c>();
        shr = getResourceURL('SHR');
        List<MarkerProperty> lstMarkers = new list<MarkerProperty>();
        for (WQMSession__c W : [
                Select Id, Site__c, Site__r.Name, Site__r.Station_Nr__c,
                        Site__r.LongLat__Latitude__s, Site__r.LongLat__Longitude__s, Season__c, Avg_IBI_Score__c, (Select Id, Critters__r.Name, Count__c From Session_Organisms__r), Mon_Date_Time__c
                FROM WQMSession__c
                WHERE Season__c = '1-SPRING' AND CALENDAR_YEAR(Mon_Date_Time__c) >= :startYear
                AND CALENDAR_YEAR(Mon_Date_Time__c) <= :endYear
                ORDER BY Mon_Date_Time__c DESC
                LIMIT 500
        ]) {
            lstMarkers.add(new MarkerProperty(W));
        }
        return lstMarkers;
    }

    public class MarkerProperty {
        public String source { get; set; }
        public string MarkerImage { get; set; }
        public boolean isDisplayInfoWindow { get; set; }
        public string InfoWindowContentString { get; set; }
        public Coordinates position { get; set; }
        public string RecordID { get; set; }
        public decimal Score { get; set; }

        public MarkerProperty(WQMSession__c W) {
            source = 'WQM';
            isDisplayInfoWindow = true;
            position = new Coordinates(W.Site__r.LongLat__Longitude__s, W.Site__r.LongLat__Latitude__s);
            String streamHealthText = '';
            String scoreColor = colorPoor;
            Score = w.Avg_IBI_Score__c;
            system.debug('w.Avg_IBI_Score__c is ' + Score);
            if (Score <= 2.1) {
                // Poor
                streamHealthText = 'Poor';
                scoreColor = colorPoor;
            } else if (Score > 2.1 && Score <= 3.1) {
                // Fair
                streamHealthText = 'Fair';
                scoreColor = colorFair;
            } else if (Score > 3.1 && Score <= 4.3) {
                // Good
                streamHealthText = 'Good';
                scoreColor = colorGood;
            } else if (Score > 4.5) {
                // Excellent
                streamHealthText = 'Excellent';
                scoreColor = colorExcellent;
            } else {
                streamHealthText = 'Not Found';
                scoreColor = colorPoor;
                Score = 0.0;
            }

            String arrow = shr + '/images/arrow_up.png';
            String no_photo = shr + '/images/no_photo.png';
            string ResultURL1 = System.URL.getOrgDomainUrl().toExternalForm();
            string PR = string.ValueOf(PageReference.forResource('SHR_Color_Bar').getURL());
            string trimmedPR = PR.indexOf('?') == -1 ? PR : PR.subString(0, PR.indexOf('?'));
            String meter = ResultURL1 + trimmedPR;
            InfoWindowContentString = '<div><img src="' + meter + '" class="meter" /><span class="arrow" style="margin-left:' + getLeftOffsetWQM(Score) + 'px;"><img src="' + arrow + '" /></span></div>';
            InfoWindowContentString += '<div>';
            InfoWindowContentString += '<h5>Score:</h5><div class="well well-sm">' + W.Avg_IBI_Score__c + '</div>';
            InfoWindowContentString += '<h5>WQM Session Id:</h5><div class="well well-sm">' + W.Id + '</div>';
            InfoWindowContentString += '<h5>Site:</h5><div class="well well-sm">' + W.Site__r.Name + '</div>';
            if (W.Mon_Date_Time__c != null) {
                InfoWindowContentString += '<h5>Date & Time:</h5><div class="well well-sm">' + W.Mon_Date_Time__c.format('MM/dd/yyyy hh:mm a') + '</div>';
            }
            InfoWindowContentString += '<h5>Season:</h5><div class="well well-sm">' + W.Season__c + '</div>';
            // InfoWindowContentString += '<h5>Stream Health Score:</h5><div class="well well-sm">' + streamHealthText + '</div>';
            InfoWindowContentString += '<div class="panel panel-default"><div class="panel-heading"><h5>Organisms Found</h5></div>';
            InfoWindowContentString += '<ul class="list-group touch-scroll">';
            // list for organisms
            if (W.Session_Organisms__r == null || W.Session_Organisms__r.size() == 0) {
                InfoWindowContentString += '<li class="list-group-item">Data not found.</li>';
            } else {
                for (Session_Organisms__c sesOrg : W.Session_Organisms__r) {
                    InfoWindowContentString += '<li class="list-group-item"><span class="badge">' + sesOrg.Count__c + '</span> ' + sesOrg.Critters__r.Name + '</li>';
                }
            }
            InfoWindowContentString += '</ul></div>';
            InfoWindowContentString += '<h6><a target="_blank" href="/apex/DisplayGraphPage?id=' + W.Site__c + '">Stream Health Over Time</a></h6>';
            InfoWindowContentString += '</div>';
            MarkerImage = GetMarkerImage_WQM(Score);
            RecordID = W.Id;
        }

        public MarkerProperty(CritterSurveyFromMobile__c C, Critters__c[] critterList) {
            source = 'CritterSurvey';
            isDisplayInfoWindow = true;
            position = new Coordinates(C.SurveyLat__c, C.SurveyLng__c);
            InfoWindowContentString = '<div>';
            InfoWindowContentString = InfoWindowContentString + '<p><b>Site:</b> ' + C.Site__c + '</p>';

            String streamHealthText = '';
            Score = C.SensitivityScore__c;
            String scoreColor = colorPoor;
            if (C.SensitivityScore__c <= 10) {
                // Poor
                streamHealthText = 'Poor';
                scoreColor = colorPoor;
            } else if (C.SensitivityScore__c >= 11 && C.SensitivityScore__c <= 15) {
                // Fair
                streamHealthText = 'Fair';
                scoreColor = colorFair;
            } else if (C.SensitivityScore__c >= 16 && C.SensitivityScore__c <= 20) {
                // Good
                streamHealthText = 'Good';
                scoreColor = colorGood;
            } else if (C.SensitivityScore__c >= 21) {
                // Excellent
                streamHealthText = 'Excellent';
                scoreColor = colorExcellent;
            } else {
                streamHealthText = 'Not Found';
                scoreColor = colorPoor;
            }

            InfoWindowContentString = '';
            // On 16 Dec I figure out how to generate the URL
            string ResultURL1 = System.URL.getOrgDomainUrl().toExternalForm();
            string PR = string.ValueOf(PageReference.forResource('SHR_Color_Bar').getURL());
            string trimmedPR = PR.indexOf('?') == -1 ? PR : PR.subString(0, PR.indexOf('?'));
            String meter = ResultURL1 + trimmedPR;
            system.debug('Meter just calculated as ' + meter);
            String arrow = shr + '/images/arrow_up.png';
            String no_photo = shr + '/images/no_photo.png';
            system.debug('Score is ' + Score);
            InfoWindowContentString = '<div><img src="' + meter + '" class="meter" /><span class="arrow" style="margin-left:' + getLeftOffset(Score.intValue()) + 'px;"><img src="' + arrow + '" /></span></div>';
            InfoWindowContentString += '<h5>Stream:</h5><div class="well well-sm">' + C.Name + '</div>';
            if (C.SurveyDateTime__c != null) {
                InfoWindowContentString += '<h5>Date & Time:</h5><div class="well well-sm">' + C.SurveyDateTime__c.format('MM/dd/yyyy hh:mm a') + '</div>';
            }
            //InfoWindowContentString += '<h5>Team Members:</h5><div class="well well-sm">' + C.Number_in_Group__c+ '</div>';
            //InfoWindowContentString += '<h5>Stream Health:</h5><div class="well well-sm" style="color: '+scoreColor+';">' + streamHealthText + '</div>';
            InfoWindowContentString += '<div class="panel panel-default"><div class="panel-heading"><h5>Critters Found</h5></div>';
            InfoWindowContentString += '<ul class="list-group touch-scroll">';
            if (C.CritterIDs__c != null) {
                String[] critterIds = null;
                if (C.CritterIDs__c.contains('|')) {
                    critterIds = C.CritterIDs__c.split('\\|');
                } else {
                    critterIds = C.CritterIDs__c.split(',');
                }
                if (critterIds != null && critterIds.size() > 0) {
                    Set<String> ids = new Set<String>();
                    for (String ccName : critterIds) {
                        if (!critters.containsKey(ccName)) {
                            ids.add(ccName);
                        }
                    }
                    if (ids.size() > 0) {
                        // query only if required
                        for (Critters__c critter : critterList) {
                            critters.put(critter.CCName__c, critter);
                        }
                    }
                    for (String cid : critterIds) {
                        Critters__c critter = critters.get(cid);
                        if (critter != null) {
                            InfoWindowContentString += '<li class="list-group-item">' + critter.Common_Name__c + '</li>';
                        } else {
                            InfoWindowContentString += '<li class="list-group-item">' + cid + '</li>';
                        }
                    }
                } else {
                    InfoWindowContentString += '<li class="list-group-item">No critter found!</li>';
                }
            } else {
                InfoWindowContentString += '<li class="list-group-item">No critter found!</li>';
            }
            InfoWindowContentString += '</ul></div></div>';
            system.debug('INfoWindowContentSTring is ' + InfoWindowContentString);
            MarkerImage = GetMarkerImage(C.SensitivityScore__c);
            RecordID = C.Id;
        }

        private String GetMarkerImage(Decimal val) {
            if (val <= 11) {
                return 'critter_red';
            } else if (val <= 16) {
                return 'critter_yellow';
            } else if (val <= 21) {
                return 'critter_green';
            } else {
                return 'critter_blue';
            }
        }

        private String GetMarkerImage_WQM(Decimal val) {
            if (val == null) {
                return 'wqm_no_score';
            } else if (val <= 2.1) {
                return 'wqm_poor';
            } else if (val <= 3.2) {
                return 'wqm_fair';
            } else if (val <= 4.5) {
                return 'wqm_good';
            } else {
                return 'wqm_excellent';
            }
        }
    }

    public class Coordinates {
        public Decimal Lat { get; set; }
        public Decimal Lon { get; set; }

        public Coordinates(Decimal Latitude, Decimal Longitude) {
            Lat = Latitude;
            Lon = Longitude;
        }

    }

    public static String getResourceURL(String resourceName) {
        List<StaticResource> resourceList = [SELECT Name, NamespacePrefix, SystemModStamp FROM StaticResource WHERE Name = :resourceName];
        if (resourceList.size() == 1) {
            String namespace = resourceList[0].NamespacePrefix;
            return '/resource/' + resourceList[0].SystemModStamp.getTime() + '/'
                    + (namespace != null && namespace != '' ? namespace + '__' : '')
                    + resourceName;
        } else return '';
    }

    public static Integer getLeftOffset(Decimal score) {
        system.debug('In getLeftOffset Score is ' + Score);
        Integer width = 215;
        Integer xpos = 14;
        Decimal step = (width - xpos) / 15;
        if (score <= 9) {
            xpos = 14;
        } else if (score >= 23) {
            xpos = 215;
        } else {
            xpos = integer.valueOf(14 + ((score - 9) * step));
        }
        system.debug('In getLeftOffset XPOS is ' + xpos);
        return xpos;
    }

    public static Decimal getLeftOffsetWQM(Decimal score) {
        // Created for Cathy on 12/20/2019
        system.debug('In getLeftOffsetWQM Score is ' + Score);
        Integer width = 215;
        Decimal xpos = -3;
        xpos = -3 + ((score - 1) * 55);
        system.debug('In getLeftOffset XPOS is ' + xpos);
        return xpos;
    }

}