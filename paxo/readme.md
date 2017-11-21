### Installation

Suggested in a virtualenv as always:
> pip install -r requirements.txt 



### Usage

Edit and run clientOperations.py: 

First create a raw score with  
> scoreOntologies(sourceOntology, targetOntology)

Calculate a calculatedScore with:
> calculatePrimaryScore(combinedOntologyName, params, writeToDisc)

or calculate and validated a primary score with:
> calculateAndValidateOntologyPrimaryScore(combinedOntologyName, stdName, stdFile, params, writeToDisc, parseParms)



### Prerequisite:

* A Folder “pipeline_output” has to be present
* Within this folder there need to be a folder to put the calculated primary score (e.g. the folder “ordo_doid” containing the file “scoring_output_ordo_doid.csv”)
* A path to a validation file if you want to validate against a std file