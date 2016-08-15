/** 
Poem Format: For defining poetic forms, from Haiku to Sonnets.
It can also express a poem! This allows for the use of templates.

Design Aims:

 - Simpler than a formal grammar.
 - Able to describe a wide range of poems. 
 - Suitable for use in AI generation systems.
 - ..including collaborative systems.
 - Support variables (e.g. "rhyme-scheme ABAB") and specific instantiations (e.g. "must use an -ee rhyme").
 - Similar where possible to descriptions already used by poets.
 
@author Daniel Winterstein, 2016
@version 0.1.1

*All properties are optional.*
*
This spec uses a JSDoc markup to describe property types and values (see http://usejsdoc.org/tags-type.html)

// Comments are allowed in .spec.json files, by using //. They are removed in processing (NB: and are not re-inserted in the output json).

References:

 - http://www.poetryarchive.org/glossary
 
Note: Before creating this, I did look for something similar already in existence -- couldn't
find it. I also toyed with a domain specific language instead of javascript
-- but I couldn't get it to carry enough info without becoming messy.
 
Some formats:

 - Haiku
 - Villanelle
 - Sonnet
 - Ballade https://www.britannica.com/art/ballade#ref230433
*/

/**
@typedef {string} Variable 
	If all upper-case, this is unset -- e.g. used to indicate rhyme-schemes. Must be a single alphanumeric token.
	If lower-case or mixed-case, this is a specific value.
*/

/**
@typedef {Object} Poem
@property @class {string} Always "Poem", this is just to aid readability.
@property id {string} A unique id for this poem.
@property name {string} The poem's name.
@property format {string} The name of the format, e.g. "haiku"
@property author {string} The author's name.
@property desc {string} A description of the poem.
@property date {(Date|string)}
@property verses {Verse[]} The verses. If there is only one verse, then this can be omitted and replaced directly by the lines property.
@property topic {string} This can also be specified at the Verse and Line level.
@property reference {string|string[]} A topic which should be referenced. 
	This constraint is similar to topic, but a reference may be in passing, whilst topic should match the heart of the poem. 
	This can also be specified at the Verse and Line level. 
@property emotion {string} This can also be specified at the Verse and Line level.
@property feet {number} The number of feet (stressed syllables) per line. 
		This can also be specified at the Verse and Line level.
@property stress {string} The pattern of stress per line -- using "s" and "u" for stressed and unstressed. E.g. iambic pentameter is: "ususususus"
		This can also be specified at the Verse and Line level.
@property weight {object} For specifying that some constraints can be broken. 
		This also provides a guide to evaluating a poem - the weightings could be used to say which factors matter more when scoring poems.
		A map of constraint property names (e.g. topic, stress) to a [0, 1] weighting.
		0 means "of no importance", 1 means "must be followed". 
		The interpretation inbetween is up to the implementation - a harmonic mean is suggested.
		Constraint property names are those used to specify poem form, plus a couple of extra properties: 
			rhyme, refrain, stress, feet, syllables -- or "form" which covers all of these,
			topic, emotion, reference,
			grammar (should the poem be grammatical?),
			sense (should the poem make "normal" prosaic sense? This might be approximately evaluated by an n-gram model),
@property comment {Comment} Can be set at the Poem, Verse, or Line level. 
@property request {Request} Can be set at the Poem, Verse, or Line level. For controlling the flow of collaboration.
	If no requests are set, then default behaviour applies (either "write the whole poem" or "score the whole poem")
	If any requests are present in the Poem, then they are all that the recipient should do.	 			
*/

/**
@typedef {Object} Verse
@property @class {string} Always "Verse", this is just to aid readability.
@property lines {Line[]}
@property num {number} It can be helpful to number verses.
*/

/**
@typedef {Object} Line
@property @class {string} Always "Line"; this is just to aid readability. 
@property rhyme {Variable} If specific, then the International Phonetic Alphabet (IPA) is the preferred format.
@property syllables {number}
@property refrain {Variable} Repeated lines should share the same refrain variable.
@property start_refrain {Variable} Repeated lines should begin the same way.
@property end_refrain {Variable} Repeated lines should end the same way.
@property text {string}
@property tokens {Token[]} This allows for fine-grained constraints, such as "start the sentence with 'If'".
@property num {number} It can be helpful to number lines.
*/

/**
@typedef {Object} Token A single word. Most constraints can be specified at the individual word level, if so desired.
Can be an empty object for a blank placeholder.
Unless text or words are set, a Token can expand to any number of words! 
This is the case even when pos is set -- e.g. a noun could become a noun-phrase.
@property rhyme {Variable}
@property syllables {number}
@property feet {Variable}
@property stress {Variable}
@property words {number} If set, this token represents this many words, no more, no less.
@property text {string}
@property pos {string} part-of-speech. Brown Corpus POS-tags are the preferred format.
*/

/**
@typedef {Object} Comment A comment
@property author {string}
@property text {string}
*/

/**
@typedef {Object} Request A writing request, to say "please write this verse", for collaborative writing
@property to {string} Who is this request to?
@property action {string} The requested action -- "write" or "score"
@property response {object} Where the agent requested puts details on their response (as well as modifying the poem). 
*/


/**
 * Future features:
 * 
 *  - alliteration
 *  - metre: other than English-style feet
 *  - voice: 1st "I", 2nd "you", 3rd "he"
 *  - kenning
 *  - sibilance
 *  - assonance
 *  - Reading/rhythm markers, such as caesure
 */

