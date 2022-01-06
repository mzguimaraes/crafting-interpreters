package com.craftinginterpreters.lox;

/** Interface describing Lox objects that can get and set values as member variables or methods.
 *  This is intended to be implemented for both instance and static fields and methods.
 */
public interface MemberStore {

   /**
    * Gets the member stored for this token's lexeme in this collection.
    * @param name Token object representing the name of the member being looked up.
    * @return the Object stored under the given name.
    * @throws RuntimeError if there is nothing stored under the given name.
    */
   public Object get(Token name);

   /**
    * Sets a name-object pair in the field store.
    * @param name Token object representing the name of the member.
    * @param value Value to be stored.
    */
   public void set(Token name, Object value);

   @Override
   public String toString();
}
