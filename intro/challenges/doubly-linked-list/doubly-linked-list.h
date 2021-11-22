#include<stdio.h>
#include<stdlib.h>
#include<string.h>
#include<stdbool.h>

// Node within a doubly linked list, storing string data.
struct DoublyLinkedNode {
  struct DoublyLinkedNode *prev;
  struct DoublyLinkedNode *next;

  char *data; //points to first char of char array
  size_t datac; //length of data char array
};

// An ordered sequence of nodes where each node holds string data 
// and points to both its previous and following neighbors.
struct DoublyLinkedList {
  struct DoublyLinkedNode *head;
  struct DoublyLinkedNode *tail;
};

// creates a node on the heap by reading size characters from input character array.
struct DoublyLinkedNode *create_node(char input[], size_t size);

// initializes list with provided node. first will be set as the new list's head AND tail
struct DoublyLinkedList create_list(struct DoublyLinkedNode *first);


// frees all memory allocated for node data.
int free_node(struct DoublyLinkedNode *node);

// frees all memory allocated for node data in this list.
int free_list(struct DoublyLinkedList *list);


// formats a string describing node and writes it to dest.
// ensure dest is large enough to contain the characters used to format the node by initializing with space for at least 20 extra characters
int format_node_str(char dest[], struct DoublyLinkedNode *node);

// prints all fields of provided node.
int print_node_data(struct DoublyLinkedNode *node);

// prints the head node and the tail node of list followed by a printout of the full list.
// if flag print_list_backwards is set, the list printout will start with list->tail and read through previous nodes until it terminates at list->head.
int print_list_data(struct DoublyLinkedList *list, bool print_backwards);


// inserts new_node into list, after node's position.
int insert_after(struct DoublyLinkedList *list, struct DoublyLinkedNode *node, struct DoublyLinkedNode *new_node);

// inserts new_node into list, before node's position.
int insert_before(struct DoublyLinkedList *list, struct DoublyLinkedNode *node, struct DoublyLinkedNode *new_node);

// appends node to the head of the list.
int insert_head(struct DoublyLinkedList *list, struct DoublyLinkedNode *node);

// creates a new node from input char array, reading size characters from it, then appends that node to the head of list.
// returns the newly created node.
int insert_tail(struct DoublyLinkedList *list, struct DoublyLinkedNode *node);

// appends node to the tail of the list.
struct DoublyLinkedNode *insert_str_head(struct DoublyLinkedList *list, char input[], size_t size);

// creates a new node from input char array, reading size characters from it, then appends that node to the tail of list.
// returns the newly created node.
struct DoublyLinkedNode *insert_str_tail(struct DoublyLinkedList *list, char input[], size_t size);

// finds a node in list that has data equal to input, if such a node exists.  
// if no such node exists, returns NULL.
struct DoublyLinkedNode *find(struct DoublyLinkedList *list, char *data);


// searches for a node with the provided data in list.  if such a node is found, that node is removed from the list, maintaining continuity of links.
// if a matching node is found, returns the removed node.
// if no such node is found, returns NULL.
struct DoublyLinkedNode *remove_node_with_data(struct DoublyLinkedList *list, char *data);

// removes node from its list by pointing its neighbors to each other and setting its own neighbor pointers to NULL. 
struct DoublyLinkedNode *remove_node_from_list(struct DoublyLinkedList *list, struct DoublyLinkedNode *node);