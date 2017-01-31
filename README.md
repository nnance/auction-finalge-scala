# Auction system


The goal of the system is to serve offers to users. Using the Ports and Adaptors pattern to illustrate how a service can be easily developed and tested in isolation before being deployed as microservices.

The project is a POC that supporting the concepts outlined in the associated [BLOG](blog/POST.md) post.

### Requirements

* Return offers filtered by credit score.
* Return user with eligible offers based on the following rules:
  * (a) The user's credit score is within the offer's range
  * (b) The user's current outstanding loan is less than or equal to the auto loan offer's high limit (maximumAmount).
* Design the eligibility system to be easily extended with new rules.
* Deserialize the sample data in an asynchronous manor and service the appropriate offers from step

### Data

The json data used for validation is included in the [data.json](./fixtures/data.json) file in the fixtures folder. This data should be deserialized and processed by type.  

## Getting started

Starting the Partner Service

```sh
sbt 'runMain krs.partner.service.PartnerServer -admin.port=:9990'
```

Starting the User Service

```sh
sbt 'runMain krs.user.service.UserServer -admin.port=:9991'
```

Starting the API Service

```sh
sbt 'runMain rest.APIServer -admin.port=:9992'
```

## Testing

Confirm the services are working with the REST services:

Getting offers by credit score
```sh
curl http://localhost:8080/offers/550
```

Getting user with offers
```sh
curl http://localhost:8080/user/1
```


Load Testing
```sh
brew install wrk
wrk -t12 -c400 -d30s http://127.0.0.1:8080/user/2
```

# Architecture
System architecture:

# Item service

Manages the description and auction status (created, auction, completed, cancelled) of an item.

## Queries handled

* **getItem** - Gets an item by an ID.
* **getItemsForUser** - Gets a list of items in a provided status that are owned by a given user.

## Events emitted

* **AuctionStarted** (public) - When the auction is started, in response to **startAuction**.
* **ItemUpdated** (private) - When user editable fields on an item are updated in response to **createItem**.
* **AuctionCancelled** (private) - When the auction is cancelled, in response to **cancelAuction**.
* **AuctionFinished** (private) - When the auction is finished, in response to **BiddingFinished**.

Event emitted publically are published via a broker topic named `item-ItemEvent`.

## Commands handled

### External (user)

* **createItem** - Creates an item - emits **ItemUpdated**.
* **updateItem** - Updates user editable properties of an item, if allowed in the current state (eg, currency can't be updated after auction is started), emits **ItemUpdated**.
* **startAuction** - Starts the auction if current state allows it, emits **AuctionStarted**.
* **cancelAuction** (not supported) - Cancels the auction if current state allows it, emits **AuctionCancelled**.

## Events consumed

### Bidding service

* **BidPlaced** - Updates the current price of the item.
* **BiddingFinished** - Completes the auction if current state allows, emits **AuctionFinished**.

# Bidding service

Manages bids on items.

## Queries handled

* **getBids** - Gets all the bids for an item.

## Events emitted

* **BidPlaced** - When a bid is placed, in response to **placeBid**.
* **BiddingFinished** - When bidding has finished, in response to **finishBidding**.

## Commands handled

### External (user)

* **placeBid** - Places a bid, if the bid is greater than the current bid, emits **BidPlaced**.

### Internal

* **finishBidding** - Triggered by scheduled task that polls a read side view of auctions to finish, emits **BiddingFinished**

## Events consumed

### Item service

* **AuctionStarted** - Creates a new auction for the item
* **AuctionCancelled** - Completes an auction prematurely

# Search service

Handles all item searching.

## Queries handled

* **search** - Search for items currently under auction matching a given criteria.
* **getUserAuctions** - Gets a list of all current auctions that a user is participating in by user ID.

## Events consumed

### Item service

* **ItemUpdated** - Creates or updates the details for an item in the search index
* **AuctionStarted** - Updates the status for an item to started
* **AuctionFinished** - Deletes an item from the search index
* **AuctionCancelled** - Deletes an item from the search index

### Bidding service

* **BidPlaced** - Updates the current price for an item, if it exists in the index

# Transaction service

Handles the transaction of negotiating delivery info and making payment of an item that has completed an auction.

## Queries handled

* **getTransaction** - Gets a transaction by an items ID.
* **getTransactionsForUser** - Gets a list of all transactions that a given user is involved with.

## Events emitted

* **DeliveryByNegotiation** - When the buyer has selected by negotiation, in response to **submitDeliveryDetails**.
* **DeliveryPriceUpdated** - When the seller has updated the delivery price, in response to **setDeliveryPrice**.
* **PaymentDetailsSubmitted** - When payment details are submitted, in response to **submitPaymentDetails**.
* **PaymentConfirmed** - When payment has been confirmed, in response to payment service **ReceivedPayment**.
* **PaymentFailed** - When payment has failed, in response to payment service **FailedPayment**.
* **ItemDispatched** - When the item has been dispatched, in response to **dispatchItem**.
* **ItemReceived** - When the item has been received, in response to **receiveItem**.
* **MessageSent** - When a user has sent a message, in response to **sendMessage**.
* **RefundInitiated** - When the seller has initiated a refund, in response to **initiateRefund**.
* **RefundConfirmed** - When a refund has been confirmed.

## Commands handled

### External (user)

* **sendMessage** - Send a message to the other user, emits **MessageSent**.
* **submitDeliveryDetails** - Used by the buyer to submit delivery details, emits **DeliveryDetailsSubmitted**.
* **setDeliveryPrice** - Used by the seller to set the delivery price when delivery option is by negotiation, emits **DeliveryPriceUpdated**.
* **submitPaymentDetails** - Used by the buyer to submit payment details.
* **dispatchItem** - Used by the seller to say they have dispatched the item.
* **receiveItem** - Used by the buyer to say they have received the item.
* **initiateRefund** - Used by the seller to initiate a refund.

## Events consumed

### Item service

* **AuctionFinished** - Creates the transaction.

### Payment service

* **ReceivedPayment** - Indicates the payment has been made, emits **PaymentConfirmed**.
* **FailedPayment** - Indicates payment failed, emits **PaymentFailed**.
* **MadeRefund** - Indicates a refund was made, emits **RefundConfirmed**.