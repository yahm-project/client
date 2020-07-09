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
       isNewDataNeeded(LastInfo, Info, Radius).

+actualRadius(Radius)
    <- -+radius(Radius).

+fetch
    <- !fetchNewData.

+!fetchNewData : pos(Info) & radius(Radius)
    <- -+lastPositionFetched(Info);
       updateConditions(Info,Radius).

+qualities(Qualities)
    <- println(Qualities).