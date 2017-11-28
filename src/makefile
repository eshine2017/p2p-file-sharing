JC = javac
JFLAGS = -g
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
	./Communication/Handshake.java \
	./Communication/Message.java \
	./FileProcessing/FileProcess.java \
	./FileSharing/Common.java \
	./FileSharing/Neighbor.java \
	./FileSharing/PeerToPeer.java \
	./FileSharing/Chock.java \
	./FileSharing/Host.java \
	peerProcess.java

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class
	$(RM) ./Communication/*.class
	$(RM) ./FileProcessing/*.class
	$(RM) ./FileSharing/*.class