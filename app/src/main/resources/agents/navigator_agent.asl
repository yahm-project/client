!assist_user.

+!assist_user
	<-  makeArtifact("NavigatorGUI", "it.unibo.yahm.client.artifacts.NavigatorGUIArtifact",[],NavigatorActivity);
    	focus(NavigatorActivity);
    	makeArtifact("RoadInfoArtifact", "it.unibo.yahm.client.artifacts.RoadInfoArtifact",[],RoadInfoArtifact);
        focus(RoadInfoArtifact).

+!observeGPS
    <-  makeArtifact("GPSArtifact", "it.unibo.yahm.client.artifacts.GPSArtifact",[],GPSArtifact);
        focus(GPSArtifact).

+ui_ready [artifact_name(Id,NavigatorActivity)]
    <-  println("MainUI ready.");
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

+!checkIfNewDataIsNeeded : lastPositionFetched(LastInfo) & radius(Radius) & gpsInfo(Info) & isSupportEnable(true)
    <- isNewDataNeeded(LastInfo, Info, Radius).

+!fetchNewData : gpsInfo(Info) & radius(Radius) & isSupportEnable(true)
    <- -+lastPositionFetched(Info);
       updateConditions(Info,Radius).

+!checkForAlarm : gpsInfo(Info) & obstacles(Obstacles) & isSupportEnable(true)
    <- checkAlarm(Obstacles, Info).

+alarmNeeded(ObstacleType)
    <- emitAlarm(ObstacleType).

+qualities(Qualities)
    <- updateQualities(Qualities).

+obstacles(Obstacles)
    <- updateObstacles(Obstacles).