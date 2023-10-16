# Requirement

## What's New
Compared to UDP, GUDP has
- Sliding window flow control
- Detection and **ARQ(automatic repeat request)-based** retransmission of lost or reordered packets

Compared to TCP, GUDP has difference
- GUDP transfer datagrams, while TCP transfer byte streams.
- GUDP is connection-less communication, while TCP is connection-oriented.
  - TCP requires that the client application establishes a connection, and the server accepts the connection, before communication. GUDP does not have the notion of connections.
- GUDP is unidirectional, while a TCP connection is bidirectional.


## GUDP Header
<table>
    <tr>
        <td>Bit 0-15</td>
        <td>Bit 16-31</td>
    <tr>
    <tr>
        <td>Version</td>
        <td>Type</td>
    <tr>
    <tr>
        <td colspan="2">Sequence Number</td>
    <tr>
</table>

- **Version**: version of the GUDP protocol. The current version is version 1.
- **Type**: GUDP packet type (DATA, BSN, and ACK; see below)
- **Sequence Number**: Datagram sequence number


## GUDP Protocol Description

### DATA Packets
DATA packets (type field = 1) carry application data; the sequence number increases with one for each DATA packet sent from the sender to the receiver. So, unlike TCP, GUDP numbers packets, not bytes.

### BSN Packets
BSN (Base Sequence Number) packets (type field = 2) are used to control sequence numbers. Like TCP, GUDP initial sequence numbers are randomised, and the BSN packet is used by the sender to tell the receiver the Base Sequence Number – the starting point of the sequence numbers for DATA packets. The sequence number for DATA packets starts with one plus the BSN and consecutively increases by one for each new DATA packet sent.

### ACK packets
ACK packets (type field = 3) acknowledge the reception of DATA packets. The sequence number field in an ACK packet contains the sequence number of the last DATA packet received by the receiver UDP endpoint, plus one. In other words, the sequence number field is the sequence number of the DATA packet the receiver expects to receive next. 

ACK packets are also used by the receiver UDP endpoint to acknowledge the reception of BSN packets. So when a receiver gets a BSN packet,  the receiver responds with an ACK packet that contains the sequence number from the BSN packet plus one (since that is the sequence number of the DATA packet that the receiver is now expecting).

### Timers
If an ACK is not received within a certain time after that the DATA packet was sent, the DATA packet is considered to be lost and should be retransmitted. The sender can attempt to retransmit a packet up to a certain number of times. After that, communication has failed, and the application should be informed.

The same applies to BSN packets – if no ACK is received within a certain time, communication has failed. 

### Windows
GUDP maintains a window size, which is the maximum number of outstanding DATA packets that have not been acknowledged. So when the sender has reached the window size, it cannot send any more packets to the receiver. Instead, it has to wait for an ACK packet or a time-out.

## GUDP Socket Application Programming Interface (API)
'''

'''