!start.

+!start
	<-  makeArtifact("NavigatorGUI", "it.unibo.yahm.client.activities.NavigatorGUIArtifact",[],NavigatorActivity);
    	focus(NavigatorActivity).

+!observeGPS
    <-  makeArtifact("GPSArtifact", "it.unibo.yahm.client.entities.GPSArtifact",[],GPSArtifact);
        focus(GPSArtifact).

+!observeRoadInfo
    <-  makeArtifact("RoadInfoArtifact", "it.unibo.yahm.client.entities.RoadInfoArtifact",[],RoadInfoArtifact);
        focus(RoadInfoArtifact).

+ui_ready [artifact_name(Id,NavigatorActivity)]
    <-  println("MainUI ready.");
        !observeRoadInfo;
        !observeGPS.

+gpsInfo(Info) : not(lastPositionFetched(LastInfo))
    <- updatePosition(Info);
       !fetchNewData.

+gpsInfo(Info) : lastPositionFetched(LastInfo) & radius(Radius)
    <- updatePosition(Info);
       !checkIfNewDataIsNeeded;
       !checkForAlarm.

+fetch
    <- !fetchNewData.

+!checkIfNewDataIsNeeded : lastPositionFetched(LastInfo) & radius(Radius) & gpsInfo(Info)
    <- isNewDataNeeded(LastInfo, Info, Radius).

+!fetchNewData : gpsInfo(Info) & radius(Radius)
    <- -+lastPositionFetched(Info);
       updateConditions(Info,Radius).

+!checkForAlarm : gpsInfo(Info) & obstacles(Obstacles)
    <- checkAlarm(Obstacles, Info).

+alarmNeeded(ObstacleType)
    <- emitAlarm(ObstacleType).

+qualities(Qualities)
    <- updateQualities(Qualities).

+obstacles(Obstacles)
    <- updateObstacles(Obstacles).