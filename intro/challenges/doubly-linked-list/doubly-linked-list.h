#include<stdio.h>
#include<stdlib.h>
#include<string.h>
#include<stdbool.h>

struct DoublyLinkedNode {
  struct DoublyLinkedNode *prev;
  struct DoublyLinkedNode *next;

  char *data; //points to first char of char array
  size_t datac; //length of data char array
};

struct DoublyLinkedList {
  struct DoublyLinkedNode *head;
  struct DoublyLinkedNode *tail;
};

struct DoublyLinkedNode *create_node(char input[], size_t size);
struct DoublyLinkedList create_list(struct DoublyLinkedNode *first);

int free_node(struct DoublyLinkedNode *node);
int free_list(struct DoublyLinkedList *list);

int format_node_str(char dest[], struct DoublyLinkedNode *node);
int print_node_data(struct DoublyLinkedNode *node);
int print_list_data(struct DoublyLinkedList *list, bool print_backwards);

int insert_after(struct DoublyLinkedList *list, struct DoublyLinkedNode *node, struct DoublyLinkedNode *new_node);
int insert_before(struct DoublyLinkedList *list, struct DoublyLinkedNode *node, struct DoublyLinkedNode *new_node);
int insert_head(struct DoublyLinkedList *list, struct DoublyLinkedNode *node);
int insert_tail(struct DoublyLinkedList *list, struct DoublyLinkedNode *node);
struct DoublyLinkedNode *insert_str_head(struct DoublyLinkedList *list, char input[], size_t size);
struct DoublyLinkedNode *insert_str_tail(struct DoublyLinkedList *list, char input[], size_t size);

struct DoublyLinkedNode *find(struct DoublyLinkedList *list, char *data);

struct DoublyLinkedNode *remove_node_with_data(struct DoublyLinkedList *list, char *data);
struct DoublyLinkedNode *remove_node_from_list(struct DoublyLinkedList *list, struct DoublyLinkedNode *node);