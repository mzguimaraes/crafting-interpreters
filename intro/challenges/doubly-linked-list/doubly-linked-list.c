#include "doubly-linked-list.h"

// TODO: research C testing and write tests

// creates a node on the heap by reading size characters from input character array.
struct DoublyLinkedNode *create_node(char *input, size_t size) {
  struct DoublyLinkedNode *node = (struct DoublyLinkedNode*)malloc(sizeof(struct DoublyLinkedNode)); 

  char *data = (char*)malloc(sizeof(char) * size);
  strncpy(data, input, size);
  node->data = data;
  node->datac = size;

  node->next = NULL;
  node->prev = NULL;

  return node;
}

// initializes list with provided node. first will be set as the new list's head AND tail
struct DoublyLinkedList create_list(struct DoublyLinkedNode *first) {
  struct DoublyLinkedList list;
  list.head = first;
  list.tail = first;
  return list;
}

// frees all memory allocated for node data in this list.
int free_list(struct DoublyLinkedList *list) {
  struct DoublyLinkedNode *curr = list->head; 
  while (curr != NULL) {
    struct DoublyLinkedNode *next = curr->next;
    free_node(curr);
    curr = next;
  }
  return 0;
}

// frees all memory allocated for node data.
int free_node(struct DoublyLinkedNode *node) {
  free(node->data);
  free(node);
  return 0;
}

// formats a string describing node and writes it to dest.
// ensure dest is large enough to contain the characters used to format the node by initializing with space for at least 20 extra characters
int format_node_str(char dest[], struct DoublyLinkedNode *node) {
  char format_str[] = "node data: %s";
  int format_strc = sizeof format_str / sizeof format_str[0];
  snprintf(dest, node->datac + format_strc, "node data: %s", node->data);
  return 0;
}

// prints all fields of provided node.
int print_node_data(struct DoublyLinkedNode *node) {
  printf("\nnode:");
  if (!node) {
    printf("\n\tNULL");
    return 1;
  }
  printf("\n\tdata: %s", node->data);
  printf("\n\tdatac: %zu", node->datac);
  printf("\n\tprev: %p", node->prev);
  printf("\n\tnext: %p", node->next);

  return 0;
}

// prints the head node and the tail node of list followed by a printout of the full list.
// if flag print_list_backwards is set, the list printout will start with list->tail and read through previous nodes until it terminates at list->head.
int print_list_data(struct DoublyLinkedList *list,  bool print_list_backwards) {
  printf("\nlist data:");
  if (!list) {
    printf("\n\tNULL");
    return 1;
  }

  printf("\nHEAD:");
  print_node_data(list->head);
  printf("\n\nTAIL:");
  print_node_data(list->tail);

  printf("\n\nfull list:");
  struct DoublyLinkedNode *curr;
  if (print_list_backwards) {
    curr = list->tail;
  } else {
    curr = list->head; 
  }
  while (curr != NULL) {
    char curr_str[curr->datac + 20];
    format_node_str(curr_str, curr);
    printf("\n\t%s", curr_str);
    if (print_list_backwards) {
      curr = curr->prev;
    } else {
      curr = curr->next;
    }
  }

  return 0;
}

// inserts new_node into list, after node's position.
int insert_after(struct DoublyLinkedList *list, struct DoublyLinkedNode *node, struct DoublyLinkedNode *new_node) {
  struct DoublyLinkedNode *after_both = node->next;
  new_node->next = after_both;
  new_node->prev = node;
  node->next = new_node;
  if (after_both == NULL) {
    list->tail = new_node;
  } else {
    after_both->prev = new_node;
  }

  return 0;
}

// inserts new_node into list, before node's position.
int insert_before(struct DoublyLinkedList *list, struct DoublyLinkedNode *node, struct DoublyLinkedNode *new_node) {
  struct DoublyLinkedNode *before_both = node->prev;
  new_node->next = node;
  new_node->prev = before_both;
  node->prev = new_node;
  if (before_both == NULL) {
    list->head = new_node;
  } else {
    before_both->next = new_node;
  }
  
  return 0;
}

// appends node to the head of the list.
int insert_head(struct DoublyLinkedList *list, struct DoublyLinkedNode *node) {
  return insert_before(list, list->head, node);
}

// creates a new node from input char array, reading size characters from it, then appends that node to the head of list.
// returns the newly created node.
struct DoublyLinkedNode *insert_str_head(struct DoublyLinkedList *list, char input[], size_t size) {
  struct DoublyLinkedNode *node = create_node(input, size);
  insert_head(list, node);
  return node;
}

// appends node to the tail of the list.
int insert_tail(struct DoublyLinkedList *list, struct DoublyLinkedNode *node) {
  return insert_after(list, list->tail, node);
}

// creates a new node from input char array, reading size characters from it, then appends that node to the tail of list.
// returns the newly created node.
struct DoublyLinkedNode *insert_str_tail(struct DoublyLinkedList *list, char input[], size_t size) {
  struct DoublyLinkedNode *node = create_node(input, size);
  insert_tail(list, node);
  return node;
}

// finds a node in list that has data equal to input, if such a node exists.  
// if no such node exists, returns NULL.
struct DoublyLinkedNode *find(struct DoublyLinkedList *list, char *data) {
  struct DoublyLinkedNode *curr = list->head;
  while (curr != NULL) {
    if (strcmp(curr->data, data) == 0) {
      return curr;
    }
    curr = curr->next;
  }
  return NULL;
}

// searches for a node with the provided data in list.  if such a node is found, that node is removed from the list, maintaining continuity of links.
// if a matching node is found, returns the removed node.
// if no such node is found, returns NULL.
struct DoublyLinkedNode *remove_node_with_data(struct DoublyLinkedList *list, char *data) {
  struct DoublyLinkedNode *found = find(list, data);

  if (found == NULL) {
    return found;
  }

  return remove_node_from_list(list, found);
}

// removes node from its list by pointing its neighbors to each other and setting its own neighbor pointers to NULL. 
struct DoublyLinkedNode *remove_node_from_list(struct DoublyLinkedList *list, struct DoublyLinkedNode *node) {
  if (node == list->head) {
    list->head = node->next;
  } else {
    node->prev->next = node->next;
  }

  if (node == list->tail) {
    list->tail = node->prev;
  } else {
    node->next->prev = node->prev;
  }

  node->prev = NULL;
  node->next = NULL;
  return node;
}

int main() {
  printf("\n");

  char data[] = "Hello doubly linked list";
  size_t datac = sizeof data / sizeof data[0];
  struct DoublyLinkedNode *node = create_node(data, datac);
  // print_node_data(node);

  struct DoublyLinkedNode *next = create_node("Next node!", 11);
  struct DoublyLinkedNode *prev = create_node("Prev node!", 11);

  struct DoublyLinkedList list = create_list(node);

  insert_after(&list, list.head, next);
  insert_before(&list, list.head, prev);

  // struct DoublyLinkedNode head = create_node("New head", 9);
  // insert_head(&list, &head);

  // struct DoublyLinkedNode tail = create_node("New tail", 9);
  // insert_tail(&list, &tail);

  struct DoublyLinkedNode *head = insert_str_head(&list, "New head", 9);
  struct DoublyLinkedNode *tail = insert_str_tail(&list, "New tail", 9);

  struct DoublyLinkedNode *after_next = create_node("After next node", 16);
  insert_after(&list, node->next, after_next);

  struct DoublyLinkedNode *before_next = create_node("Before next node", 17);
  insert_before(&list, node->next, before_next);

  print_list_data(&list, true);

  // struct DoublyLinkedNode *found = find(&list, "Before next node");
  // print_node_data(found);
  // print_node_data(found->next);

  // print_node_data(head);
  // print_node_data(tail);

  // struct DoublyLinkedNode *removed = remove_node_from_list(&list, tail);
  // print_list_data(&list, true);
  // print_node_data(removed);

  struct DoublyLinkedNode *removed = remove_node_with_data(&list, "New tail");
  print_list_data(&list, true);
  print_node_data(removed);

  printf("\n\n");

  free_node(removed);
  free_list(&list);
  return 0;
}
