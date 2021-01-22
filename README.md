# ImmuniGuard
#### ImmuniGuard
ImmuniGuard is a mobile app developed as a solution to relay attacks on apps using Google Apple Exposure Notification.
The purpose of ImmuniGuard is to be integrated as a feature in a future release of Immuni app, the Italian contact tracing app.

For each contact with a GAEN-based app user, and specifically an Immuni user, ImmuniGuard locally stores:
* Its own Immuni RPI;
* The other user Immuni RPI;
* Its own GPS coordinates;
* The current timestamp.
This data is merged in a HASH, and stored locally.

Whenever an ImmuniGuard user gets diagnosed with SARS-CoV-2, they can upload all of the HASHES to the ImmuniGuard database.
Other ImmuniGuard users can download those HASHES and compare them with their own.

If the positive user had ImmuniGuard, the HASHES will confirm if other users actually physically met the positive user, or they were victim of a relay attack.

#### GAEN-relay
GAEN-relay is a mobile app developed from Vhiribarren's Beacon Simulator (https://github.com/vhiribarren/beacon-simulator-android).

We customized the code in order to detect Google Apple Exposure Notification packets, specifically ones from Immuni app. The app can duplicate those packets, alter their data, relay them to other GAEN-relay apps through an internet database and broadcast the duplicated packets.

GAEN-relay is a practical demonstration about how easily GAEN-based contact tracing apps can be exploited through relay attacks.

#### Demo!

The demo video shows the relay attack we simulated against two users, both equipped with Immuni and ImmuniGuard apps. Our malicious app sniffs an Immuni Bluetooth identifier to retrasmit it in a different location, thus completing a relay attack. We, then, show how Immuni does not distinguish between a legitimate Bluetooth identifier and a relayed one, and could trigger an incorrect exposure notification alert. On the contrary, we show how ImmuniGuard achieves this aim and detects the relay attack, while being user privacy-preserving. 

A quick overview of the developer mode can be found in this short [video](https://github.com/SPRITZ-Research-Group/ImmuniGuard/blob/main/immuniguard-demo.avi):
