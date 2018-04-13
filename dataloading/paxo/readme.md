### Prerequisite

- In order to install dependencies, first ensure that you have Python 2.7 and a corresponding version of pip.
- With pip 2.7, you need to install the prerequisite python modules listed specifically in this directory, thus:
> `pip install requirements.txt`
- Paxo tries to predict mappings between two ontologies, that are present in the Ontology Lookup Service (OLS). It's also possible to try to map a list of terms with one target ontology in OLS.

### Usage (short)

1. First create a raw score with  
> python paxo.py paxo_config.ini -s

2. Calculate a score with:
> python paxo.py paxo_config.ini -c

3. Calculate and Validated a primary score:
> python paxo.py paxo_config.ini -cv

4. Create a csv file that is compatible with oxo. Based on a previously calculated score
> python paxo.py paxo_config.ini -n

5. Create a mapping file, given not a "real" ontology but a list of terms
> python paxo.py listprocessing_config.ini -l

### More about usage
**About 1:** The primary, 'raw' score is the base for the calculation of the mapping score. This has to execute many calls to the OLS and Oxo API and can take a long time. The files created by this step thus are somewhat of a 'checkpoint'.

**About 2:** Reading in the raw score, created by the -s option, this function calculates the actual score and tries to predict mappings. The final result is strongly influenced by the parameters defined in the config file (e.g. threshold).

**About 3:** If validation files (a 'standard') is available, the calculated result can be evaluated against this standard by using the -cv flag (first a score is calculated, then validated)

**About 4:** The option -n creates a csv file that can be loaded into OxO. This option reads in file create by the -c option.

**About 5:** To create a mapping file between a list of terms and an ontology, start paxo with -l

### Parameter explanation
To run paxo it is mandatory to provide a config file with context. The dummy config files in the config folder should provide an easy start into creating your own config file. The structure of the config file for the mapping of ontologies (flag:-s,-c, -cv) and the listprocessing (flag:-l) are slightly different, most parameters are the same. Most parameters should be self-explanatory, others are described here in a few words.

#### Shared values for all configs
**StopwordsList** Words that are cut out before string compare. Candidates for this are e.g. 'the' or 'of' but could also be words you don't want to consider e.g. 'abnormality'

**fuzzyUpperLimit** Upper limit of a fuzzy label score. A score above this limit is multiplied with the fuzzyUpperFactor. A fuzzy score of 1 is the highest, so equivalent labels (exact match)

**fuzzyUpperFactor** Factor for a fuzzy label score above the fuzzy upper limit

**fuzzyLowerLimit** A fuzzy label score between the upper and lower limit is multiplied by the fuzzyLowerFactor for the final score. A score below thus limit is discarded and leads to a fuzzy score of 0.

**fuzzyLowerFactor** Factor for a fuzzy label score between the upper and the lower limit

**oxoDistanceOne** Score for a connection of distance 1 in Oxo

**oxoDistanceTwo** Score for a connection of distance 2 in Oxo

**oxoDistanceThree** Score for a connection of distance 3 in Oxo

**synFuzzyFactor** Factor to weight a fuzzy label matching of a synonym

**synOxoFactor** Weight for a link that was found in Oxo but via a synonym and not the preferred label

**bridgeOxoFactor** Weight for a bridge term

**threshold** Threshold for the mappings. Only the final score above this threshold is considered as a mapping and printed to the file


#### Config for listprocessing
**inputFile** Path to the input file, consisting of 3 rows  (ids, labels, optional synonyms)

**resultFile** /path/output-file.csv

**detailLevel** Value can be 0,1 or 2 depending on how much detail should be printed to the final file. Value 2 is the verbose mode, where one could spot alternatives to the suggested map

**delimiter** delimiter of the input file, in most cases e.g. *,*

**synonymSplitChar** delimiter of the synonyms that are located in the 3 row, could be e.g. *|* or *;* ...
