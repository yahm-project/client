!start.

+!start
	<-  makeArtifact("NavigatorGUI", "it.unibo.yahm.client.activities.NavigatorActivity",[],NavigatorActivity);
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
       -+pos(Info);
       !fetchNewData.

+gpsInfo(Info) : lastPositionFetched(LastInfo) & radius(Radius)
    <- updatePosition(Info);
       -+pos(Info);
       !checkIfNewDataIsNeeded;
       !checkForAlarm.

+actualRadius(Radius)
    <- -+radius(Radius).

+fetch
    <- !fetchNewData.

+!checkIfNewDataIsNeeded : lastPositionFetched(LastInfo) & radius(Radius) & pos(Info) & isSupportEnable(true)
    <- isNewDataNeeded(LastInfo, Info, Radius).

+!fetchNewData : pos(Info) & radius(Radius) & isSupportEnable(true)
    <- -+lastPositionFetched(Info);
       updateConditions(Info,Radius).

+!checkForAlarm : pos(Info) & myobstacles(Obstacles) & isSupportEnable(true)
    <- println(Obstacles)
       checkAlarm(Obstacles, Info).

+alarmNeeded(ObstacleType)
    <- emitAlarm(ObstacleType).

+qualities(Qualities)
    <- updateQualities(Qualities).

+obstacles(Obstacles)
    <- -+myobstacles(Obstacles);
        updateObstacles(Obstacles).

+isSupportEnable(IsEnabled)
    <- -+isSupportEnable(IsEnabled).