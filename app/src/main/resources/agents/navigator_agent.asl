!start.

+!start
	<-  makeArtifact("NavigatorGUI", "it.unibo.yahm.client.activities.NavigatorActivity",[],NavigatorActivity);
    	focus(NavigatorActivity).

+!observeGPS
    <-  makeArtifact("GPSArtifact", "it.unibo.yahm.client.entities.GPSArtifact",[],GPSArtifact);
        focus(GPSArtifact).

+ui_ready [artifact_name(Id,NavigatorActivity)]
    <-  println("MainUI ready.")
        !observeGPS.

+gpsInfo(Info)
    <-  updatePosition(Info).