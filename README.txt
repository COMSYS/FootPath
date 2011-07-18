FootPath (Infrastructureless indoor navigation for smart phones) 

Copyright (C) 2010-2011, Paul Smith <paul.smith@rwth-aachen.de>
Copyright (C) 2010-2011, Jó Ágila Bitsch <jo.bitsch@cs.rwth-aachen.de>
Copyright (C) 2010-2011, Chair of Computer Science 4, RWTH Aachen University, <klaus@comsys.rwth-aachen.de>



FREE SOFTWARE
#############

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.

IF YOU NEED ANOTHER LICENSE
###########################

If you are planning to integrate FootPath into a commercial product, please contact us for licensing options via email at:

  jo.bitsch@cs.rwth-aachen.de


IF YOU REALLY LIKE OUR SOFTWARE
###############################

Buy us a beer when the situation arises :-)


OTHER ISSUES
############

This software is currently a proof of concept. It needs some more work to make it generally useable for a non technical person.

Open issues include:
* Automatically downloading available indoor maps
* Better UI
* Looking at several possible paths at once.


HOW TO USE THIS SOFTWARE
########################
* Create your maps with JOSM and put them into the folder res/xml
    * NOTE: ways and nodes should have an attribute indoor=yes and a name
    * compare with our maps. (TODO)
* Change lines 199-204 in Loader.java to load your maps on startup 
* Run update-revfile.sh after every commit, so that log messages refer to the correct revision.
