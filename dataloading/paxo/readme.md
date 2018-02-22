### Prerequisite

- Install and active the python libs as described previously
- Paxo tries to predict mappings between two ontologies, that are present in the Ontology Lookup Service (OLS). It's also possible to try to map a list of terms with one target ontology in OLS.

### Usage

1. First create a raw score with  
> python paxo.py ../config/config.ini -s

2. Calculate a score with:
> python paxo.py ../config/config.ini -c

3. Calculate and Validated a primary score:
> python paxo.py ../config/config.ini -cv

4. Create a neo4j compatible csv file and lod it into neo4j based on a previously calculated score
> python paxo.py ../config/config.ini -n

5. Create a mapping file, given not a "real" ontology but a list of terms
> python paxo.py ../config/listprocessing_config.ini -l

### More Information
*About 1:* The primary, 'raw' score is the base for the calculation of the mapping score. This has to execute many calls to the OLS and Oxo API and can take a long time. The files created by this step thus are somewhat of a 'checkpoint'.
*About 2:* Reading in the raw score, created by the -s option, this function calculates the actual score and tries to predict mappings. The final result is strongly influenced by the parameters defined in the config file (e.g. threshold).
*About 3:* If validation files (a 'standard') is available, the calculated result can be evaluated against this standard by using the -cv flag (first a score is calculated, then validated)
*About 4:* The option -n creates a csv file that can be loaded into neo4j. This option reads in file create by the -c option.
*About 5:* To create a mapping file between a list of terms and an ontology, start paxo with -l
