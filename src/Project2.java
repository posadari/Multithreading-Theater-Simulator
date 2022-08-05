import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.Semaphore;

class Project2 {
    // wait for = acquire
    // signal = release
    static final int MAX_CUSTOMERS = 50;
    static Random rand = new Random();
    static Semaphore readyBoxOfficeAgent = new Semaphore(0);
    static Semaphore[] waitForCustomerFromBoxOffice = {new Semaphore(0), new Semaphore(0)};
    static Semaphore[] waitForBOAgentFromCustomer = {new Semaphore(0), new Semaphore(0)};
    static Semaphore readyTicketTaker = new Semaphore(0);
    static Semaphore readyConcessionWorker = new Semaphore(0);
    static Semaphore ticketRipped = new Semaphore(0);
    static Semaphore orderServed = new Semaphore(0);
    static Semaphore readyCustomerConcession = new Semaphore(0);
    static Semaphore readyCustomerTicket = new Semaphore(0);
    static Semaphore readyJoin = new Semaphore(0);
    static int customerIDForTicketTaker;
    static int customerIDForConcession;
    static String customerOrder;
    static int[] customerIDsAtBoxOfficeLine = new int[]{-1, -1};
    static String[] customerMovieAtBoxOfficeLine = new String[]{"", ""};
    static List<List<String>> movieList = new ArrayList<>();
    static Queue<String> customerQueueForConcession = new LinkedList<>();
    static boolean[] watchMovie = new boolean[]{false, false};
    static int count = 0;

    public static void main(String[] args) throws FileNotFoundException, InterruptedException {
        if (args.length == 1) {
            String fileName = args[0];
            readFile(fileName);
            Customer[] custThreads = new Customer[MAX_CUSTOMERS];
            for (int i = 0; i < MAX_CUSTOMERS; i++) {
                custThreads[i] = new Customer(i);
                custThreads[i].start();
            }

            BoxOfficeAgent[] boxOfficeAgentsThreads = new BoxOfficeAgent[2];
            for (int i = 0; i < 2; i++) {
                boxOfficeAgentsThreads[i] = new BoxOfficeAgent(i);
                boxOfficeAgentsThreads[i].start();
            }
            TicketTaker ticketTakerThread = new TicketTaker();
            ticketTakerThread.start();

            ConcessionWorker concessionWorkerThread = new ConcessionWorker();
            concessionWorkerThread.start();

            readyJoin.acquire();
            for (int i = 0; i < MAX_CUSTOMERS; i++) {
                custThreads[i].join();
                System.out.println("Joined customer " + i);
            }
            System.exit(0);
        }
    }

    static class Customer extends Thread {
        int customerID;
        public Customer(int customerID) {
            this.customerID = customerID;
        }

        public void run() {
            try {
                int random = (int) (Math.random() * movieList.size());
                List<String> temp = movieList.get(random);
                String customerMovie = temp.get(0);
                System.out.println("Customer " + customerID + " is created, buying ticket for " + customerMovie);
                int agentID = -1; // initialize agentID as -1, assuming no agent is available yet
                readyBoxOfficeAgent.acquire(); // wait for an available box office agent
                for (int i = 0; i < customerIDsAtBoxOfficeLine.length; i++) { // check if any box office agents are available
                    // if permit < 0, acquire blocks until permit > 0
                    if (customerIDsAtBoxOfficeLine[i] == -1) {
                        customerIDsAtBoxOfficeLine[i] = customerID; // save customerID and agentID
                        customerMovieAtBoxOfficeLine[i] = customerMovie; // save customer's movie and agentID
                        waitForCustomerFromBoxOffice[i].release(); // let box office agent know customer is ready
                        agentID = i; // save current agentID
                        break;
                    }
                }
               if (agentID != -1) { // if there was an available agent
                   waitForBOAgentFromCustomer[agentID].acquire();
                   customerIDsAtBoxOfficeLine[agentID] = -1; // reset values since customer is done with box office agent
                   customerMovieAtBoxOfficeLine[agentID] = "";
               }

               if (agentID != -1 && watchMovie[agentID] == true) {
                   // System.out.println("Number of permits for customer " + customerID + ": " + availableAgent.availablePermits());

                   readyCustomerTicket.release(); // customer is ready for ticket taker
                   readyTicketTaker.acquire(); // customer waits for availability of ticket taker
                   customerIDForTicketTaker = customerID;
                   System.out.println("Customer " + customerID + " in line to see ticket taker");

                   //System.out.println("Ready ticket taker" + readyTicketTaker.availablePermits());

                   // readyTicketTaker.release
                   // TODO the thread at this point should be blocked
                   ticketRipped.acquire();

                   random = rand.nextInt(2);
                   int snack = rand.nextInt(3);

                   //customerIDForConcession = -1;
                   if (random == 1) {
                       customerIDForConcession = -1;
                       String order = "";
                       switch(snack) {
                           case 0:
                               order = "Popcorn";
                               break;
                           case 1:
                               order = "Soda";
                               break;
                           case 2:
                               order = "Popcorn and Soda";
                               break;
                       }
                       customerOrder = order;
                       // TODO
                       customerQueueForConcession.add(customerID + "\n" + order);
                       readyCustomerConcession.release();
                       readyConcessionWorker.acquire();

                       System.out.println("Customer " + customerID + " is in line to buy " + order);
                       orderServed.acquire();

                       System.out.println("Customer " + customerID + " enters theater to see " + customerMovie);

                       // readyCustomerConcession.release();
                   } else {
                       System.out.println("Customer " + customerID + " enters theater to see " + customerMovie);
                   }


               }
                count++;
                // System.out.println("count is: " + count);
                if (count == MAX_CUSTOMERS) {
                    readyJoin.release();
                }
            } catch (InterruptedException e) {
                System.err.println("Error in Thread " + customerID + ": " + e);
            }

        }
    }

    static class BoxOfficeAgent extends Thread {
        int agentID;
        public BoxOfficeAgent(int agentID) {
            this.agentID = agentID;
        }
        public boolean isMovieAvailable() {
            String customerMovie = customerMovieAtBoxOfficeLine[agentID];

            for (int i = 0; i < movieList.size(); i++) {
                List<String> temp = movieList.get(i);
                // System.out.println("movie: " + temp.get(0) + " and seatCount: " + temp.get(1));
                if (temp.get(0).equals(customerMovie) && Integer.parseInt(temp.get(1)) > 0) {
                    int updatedSeats = Integer.parseInt(temp.get(1)) - 1;
                    movieList.get(i).set(1, String.valueOf(updatedSeats));
                    return true;
                }
            }
            return false;
        }

        public void run() {
            int customerServed = 0;
            while(customerServed < MAX_CUSTOMERS) {
                try {
                    readyBoxOfficeAgent.release();
                    System.out.println("Box office agent " + agentID + " is available");
                    Thread.sleep(1500);
                    waitForCustomerFromBoxOffice[agentID].acquire();
                    //readyBoxOfficeAgents.release(); //
                    System.out.println("Box office agent " + agentID + " is now serving customer " + customerIDsAtBoxOfficeLine[agentID]);
                    if (isMovieAvailable()) {
                        System.out.println("Box office agent " + agentID + " sold ticket for " + customerMovieAtBoxOfficeLine[agentID] + " to customer " + customerIDsAtBoxOfficeLine[agentID]);
                        watchMovie[agentID] = true;
                    } else {
                        System.out.println("Box office agent " + agentID + " could not sell ticket for " + customerMovieAtBoxOfficeLine[agentID] + " to customer " + customerIDsAtBoxOfficeLine[agentID]);
                        watchMovie[agentID] = false;
                    }
                    waitForBOAgentFromCustomer[agentID].release();
                    customerServed++;
                } catch (InterruptedException e) {
                    System.err.println("Error in Thread " + agentID + ": " + e);
                }
            }

        }
    }

    static class TicketTaker extends Thread {
        public TicketTaker() {
            System.out.println("Ticket taker created");
        }
        public void run() {
            int customerServed = 0;
            while(customerServed < MAX_CUSTOMERS) {
                try {
                    readyTicketTaker.release(); // let customer know ticket taker is available
                    //System.out.println("ready ticket takers # is " + readyTicketTaker.availablePermits());
                    readyCustomerTicket.acquire(); // wait for customer
                    Thread.sleep(250);
                    System.out.println("Ticket taken from customer " + customerIDForTicketTaker);
                    customerServed++;
                    ticketRipped.release();
                    //Thread.sleep(250);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static class ConcessionWorker extends Thread {
        int cust;
        String order;
        public ConcessionWorker() {
            System.out.println("Concession stand worker created");
        }

        public void run() {
            while(true) {
                try {
                    readyConcessionWorker.release(); // im ready
                    readyCustomerConcession.acquire(); // wait for customer to order;
                    String temp = customerQueueForConcession.remove();
                    cust = Integer.parseInt(temp.split("\n")[0]);
                    order= temp.split("\n")[1];
                    Thread.sleep(3000);
                    System.out.println(order + " served for Customer " + cust);
                    orderServed.release();

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void readFile(String fileName) throws FileNotFoundException {
            File file = new File(fileName);
            Scanner scanner = new Scanner(file);
            String input = "";
            while (scanner.hasNextLine()) {
                input = scanner.nextLine();
                String[] line = input.split("\\t");
                List<String> movie = new ArrayList<>();
                movie.add(line[0]);
                movie.add(line[1]);
                movieList.add(movie);
            }
        }
}


