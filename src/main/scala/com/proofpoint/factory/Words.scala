package com.proofpoint.factory

import java.util.UUID.randomUUID

import scala.util.Random

object Words {
  val colors: Vector[String] = Vector("Black", "Brown", "Red", "Green", "Yellow", "Blue", "Cyan", "Magenta", "Gold")

  val adjs: Vector[String] = Vector("Almond", "Brass", "Apricot", "Aqua", "Asparagus", "Tangerine", "Awesome", "Banana", "Bear",
    "Bittersweet", "Fast", "Blue", "Bell", "Gray", "Green", "Violet", "Red", "Pink", "Orange", "Sienna", "Cool",
    "Earthy", "Caribbean", "Elder", "Pink", "Cerise", "Cerulean", "Chestnut", "Copper", "Better", "Candy", "Cranberry",
    "Dandelion", "Denim", "Gray", "Sand", "Desert", "Eggplant", "Lime", "Electric", "Famous", "Fern", "Forest",
    "Fuchsia", "Fuzzy", "Tree", "Gold", "Apple", "Smith", "Magenta", "Indigo", "Jazz", "Berry", "Jam", "Jungle",
    "Lemon", "Cold", "Lavender", "Hot", "New", "Ordinary", "Magenta", "Frowning", "Mint", "Mahogany", "Pretty",
    "Strange", "Grumpy", "Itchy", "Maroon", "Melon", "Midnight", "Clumsy", "Better", "Smiling", "Navy", "Neon", "Olive",
    "Orchid", "Outer", "Tame", "Cheerful", "Peach", "Periwinkle", "Pig", "Pine", "Nutty", "Plum", "Purple", "Rose",
    "Salmon", "Scarlet", "Nice", "Jolly", "Great", "Silver", "Sky", "Spring", "Long", "Glow", "Set", "Happy", "Tan",
    "Thistle", "Timber", "Tough", "Torch", "Smart", "Funny", "Tropical", "Tumble", "Ultra", "White", "Wild", "Yellow",
    "Eager", "Joyous", "Jumpy", "Kind", "Lucky", "Meek", "Nifty", "Adorable", "Aggressive", "Alert", "Attractive",
    "Average", "Bright", "Fragile", "Graceful", "Handsome", "Light", "Long", "Misty", "Muddy", "Plain", "Poised",
    "Precious", "Shiny", "Sparkling", "Stormy", "Wide", "Alive", "Annoying", "Better", "Brainy", "Busy", "Clever",
    "Clumsy", "Crazy", "Curious", "Easy", "Famous", "Frail", "Gifted", "Important", "Innocent", "Modern", "Mushy",
    "Odd", "Open", "Powerful", "Real", "Shy", "Sleepy", "Super", "Tame", "Tough", "Vast", "Wild", "Wrong", "Annoyed",
    "Anxious", "Crazy", "Dizzy", "Dull", "Evil", "Foolish", "Frantic", "Grieving", "Grumpy", "Helpful", "Hungry",
    "Lazy", "Lonely", "Scary", "Tense", "Weary", "Worried", "Brave", "Calm", "Charming", "Magic", "Easer", "Elated",
    "Enchanting", "Excited", "Fair", "Fine", "Friendly", "Funny", "Gentle", "Good", "Happy", "Healthy", "Jolly", "Kind",
    "Lovely", "Nice", "Perfect", "Proud", "Silly", "Smiling", "Thankful", "Witty", "Zany", "Big", "Fat", "Great",
    "Huge", "Immense", "Puny", "Scrawny", "Short", "Small", "Tall", "Teeny", "Tiny", "Faint", "Harsh", "Loud",
    "Melodic", "Mute", "Noisy", "Quiet", "Raspy", "Soft", "Whispering", "Ancient", "Fast", "Late", "Long", "Modern",
    "Old", "Quick", "Rapid", "Short", "Slow", "Swift", "Bitter", "Fresh", "Ripe", "Rotten", "Salty", "Sour", "Spicy")

  val nouns: Vector[String] = Vector("Alligator", "Alpaca", "Antelope", "Badger", "Armadillo", "Bat", "Bear", "Bee", "Bird",
    "Bison", "Buffalo", "Boar", "Butterfly", "Camel", "Cat", "Cattle", "Cow", "Chicken", "Clam", "Cockroach", "Codfish",
    "Coyote", "Crane", "Crow", "Deer", "Dinosaur", "Velociraptor", "Dog", "Dolphin", "Donkey", "Dove", "Duck", "Eagle",
    "Eel", "Elephant", "Elk", "Emu", "Falcon", "Ferret", "Fish", "Finch", "Fly", "Fox", "Frog", "Gerbil", "Giraffe",
    "Gnat", "Gnu", "Goat", "Goose", "Gorilla", "Grasshopper", "Grouse", "Gull", "Hamster", "Hare", "Hawk", "Hedgehog",
    "Heron", "Hornet", "Hog", "Horse", "Hound", "Hummingbird", "Hyena", "Jay", "Jellyfish", "Kangaroo", "Koala", "Lark",
    "Leopard", "Lion", "Llama", "Mallard", "Mole", "Monkey", "Moose", "Mosquito", "Mouse", "Mule", "Nightingale",
    "Opossum", "Ostrich", "Otter", "Owl", "Ox", "Oyster", "Panda", "Parrot", "Peafowl", "Penguin", "Pheasant", "Pig",
    "Pigeon", "Platypus", "Porpoise", "PrarieDog", "Pronghorn", "Quail", "Rabbit", "Raccoon", "Rat", "Raven",
    "Reindeer", "Rhinoceros", "Seal", "Seastar", "Serval", "Shark", "Sheep", "Skunk", "Snake", "Snipe", "Sparrow",
    "Spider", "Squirrel", "Swallow", "Swan", "Termite", "Tiger", "Toad", "Trout", "Turkey", "Turtle", "Wallaby",
    "Walrus", "Wasp", "Weasel", "Whale", "Wolf", "Wombat", "Woodpecker", "Wren", "Yak", "Zebra", "Ball", "Bed", "Book",
    "Bun", "Can", "Cake", "Cap", "Car", "Cat", "Day", "Fan", "Feet", "Hall", "Hat", "Hen", "Jar", "Kite", "Man", "Map",
    "Men", "Panda", "Pet", "Pie", "Pig", "Pot", "Sun", "Toe", "Apple", "Armadillo", "Banana", "Bike", "Book", "Clam",
    "Mushroom", "Clover", "Club", "Corn", "Crayon", "Crown", "Crib", "Desk", "Dress", "Flower", "Fog", "Game", "Hill",
    "Home", "Hornet", "Hose", "Joke", "Juice", "Mask", "Mice", "Alarm", "Bath", "Bean", "Beam", "Camp", "Crook", "Deer",
    "Dock", "Doctor", "Frog", "Good", "Jam", "Face", "Honey", "Kitten", "Fruit", "Fuel", "Cable", "Calculator",
    "Circle", "Guitar", "Bomb", "Border", "Apparel", "Activity", "Desk", "Art", "Colt", "Cyclist", "Biker", "Blogger",
    "Anchovy", "Carp", "Glassfish", "Clownfish", "Barracuda", "Eel", "Moray", "Stingray", "Flounder", "Swordfish",
    "Marlin", "Pipefish", "Grunter", "Grunion", "Grouper", "Guppy", "Gulper", "Crab", "Lobster", "Halibut", "Hagfish",
    "Horsefish", "Seahorse", "Jellyfish", "Killifish", "Trout", "Pike", "Ray", "Razorfish", "Ragfish", "Hamster",
    "Gerbil", "Mouse", "Gnome", "Shark", "Snail", "Skilfish", "Credit", "Card", "Social", "Security", "Number")

  val states: Vector[String] = Vector("AK", "AL", "AR", "AZ", "CA", "CO", "CT", "DC", "DE", "FL", "GA", "HI", "IA", "ID", "IL", "IN",
    "KS", "KY", "LA", "MA", "MD", "ME", "MI", "MN", "MO", "MS", "MT", "NC", "ND", "NE", "NH", "NJ", "NM", "NV", "NY",
    "OH", "OK", "OR", "PA", "RI", "SC", "SD", "TN", "TX", "UT", "VA", "VT", "WA", "WI", "WV", "WY")

  val address_suffix: Vector[String] = Vector("Road", "Lane", "Street", "Circle", "Avenue", "Center")

  val months: Vector[(String, Int)] = Vector("JAN" -> 31, "FEB" -> 28, "MAR" -> 31, "APR" -> 30, "MAY" -> 31, "JUN" -> 30, "JUL" -> 31, "AUG" -> 31, "SEP" -> 30, "OCT" -> 31, "NOV" -> 30, "DEC" -> 31)

  val dlInfix: Vector[Int] = Vector(4, 9)

  private val r = Random

  //val path: String = getUserPath("src/main/resources/edm/smartid/CreditCardNumber.js").toUri.toString
  //lazy val ccnSmartId = new SmartId("CreditCardNumber", path)
  //val ccnFilter = new SmartIdFilter(ccnSmartId)

  def nextAdj: String = adjs(r.nextInt(adjs.length))

  def nextColor: String = colors(r.nextInt(colors.length))

  def nextNoun: String = nouns(r.nextInt(nouns.length))

  def nextSSN: String = {
    val prefix = (1 to 3).map(_ => r.nextInt(10)).mkString
    val infix = (1 to 2).map(_ => r.nextInt(10)).mkString
    val postfix = (1 to 4).map(_ => r.nextInt(10)).mkString
    Vector(prefix, infix, postfix).mkString
  }

  def nextCCN: String = {
    val first = (1 to 4).map(_ => r.nextInt(10)).mkString
    val second = (1 to 4).map(_ => r.nextInt(10)).mkString
    val third = (1 to 4).map(_ => r.nextInt(10)).mkString
    val fourth = (1 to 4).map(_ => r.nextInt(10)).mkString
    Vector(first, second, third, fourth).mkString
  }

  //def nextValidCCN: String = findValidCCN(nextCCN)

  def nextUUID: String = randomUUID.toString.replace("-", "")

  def nextDate: String = {
    val year = r.nextInt(100) + 1918
    val month = months(r.nextInt(months.length))
    val day = r.nextInt(month._2) + 1
    f"$year${month._1}$day%02d"
  }

  def nextIPV4: String = {
    val first = r.nextInt(255)
    val second = r.nextInt(255)
    val third = r.nextInt(255)
    val fourth = r.nextInt(255)
    s"$first.$second.$third.$fourth"
  }

  def nextLicense: String = {
    val prefix = (1 to 4).map(_ => r.nextInt(10)).mkString
    val infix = dlInfix(r.nextInt(dlInfix.length)).toString
    val suffix = (1 to 4).map(_ => r.nextInt(10)).mkString
    s"$prefix$infix$suffix"
  }

  def nextStateCode: String = states(r.nextInt(states.length))
  def nextZip: String = (r.nextInt(99999 - 11111) + 11111).toString

//  private def findValidCCN(candidate: String): String = {
//    if (ccnFilter.analyze(candidate).score > 0) candidate
//    else findValidCCN(nextCCN)
//  }

  def nextNumber: String = r.nextInt(2) match {
    case 0 => nextCCN
    case 1 => nextSSN
  }

  def nextSeparator: String = r.nextInt(3) match {
    case 0 => ""
    case 1 => " "
    case 2 => "-"
    case _ => ""
  }

  def nextWordOrNumber: String = r.nextInt(4) match {
    case 0 => nextAdj
    case 1 => nextColor
    case 2 => nextNoun
    case 3 => nextNumber
    case _ => nextNoun
  }

  def nextWord: String = r.nextInt(3) match {
    case 0 => nextColor
    case 1 => nextAdj
    case _ => nextNoun
  }

  def wordStream: Stream[String] = Stream.continually(nextWord)

  def wordOrNumberStream: Stream[String] = Stream.continually(nextWordOrNumber)

  def creditCardStream: Stream[String] = Stream.continually(nextCCN)

  //def validCreditCardStream: Stream[String] = Stream.continually(nextValidCCN)

  def columnStream(columns: Seq[(String, () => String)]): Stream[String] = {
    val ops = columns.map(_._2).toVector
    Stream.continually(ops(r.nextInt(ops.length)).apply())
  }

  def nextAddress: String = {
    val numDigits = r.nextInt(7-4) + 4
    val prefix = (1 to numDigits).map(_ => r.nextInt(10)).mkString
    val name = nextNoun
    val suffix = address_suffix(r.nextInt(address_suffix.length))
    s"$prefix $name $suffix"
  }

  def nextState: String = {
    val city = nextAdj
    val state = nextStateCode
    val zip = nextZip
    s"$city $state $zip"
  }
}
