#include <iostream>
#include <liboscar/AdvancedCellOpTree.h>
#include <liboscar/StaticOsmCompleter.h>

//file format is as follows:
//one query per line

void help() {
	std::cerr << "cat queries.txt | foq [--help|-h] [--only-text] [--only-tags] [--with-spatial] [--with-setop] [--no-setop] [--no-set-difference] --filter <oscar-data> > filtered_queries.txt" << std::endl;
}

bool onlyTextQuery(const liboscar::AdvancedCellOpTree::Node * node) {
	using Node = liboscar::AdvancedCellOpTree::Node;
	switch (node->subType) {
	case Node::SET_OP:
	{
		bool otq = true;
		for(auto cn : node->children) {
			otq = otq && onlyTextQuery(cn);
		}
		return otq;
	}
	case Node::STRING:
	case Node::STRING_ITEM:
	case Node::STRING_REGION:
		return true;
	default:
		return false;
	};
}

bool onlyTagsQuery(const liboscar::AdvancedCellOpTree::Node * node) {
	using Node = liboscar::AdvancedCellOpTree::Node;
	switch (node->subType) {
	case Node::SET_OP:
	{
		bool otq = true;
		for(auto cn : node->children) {
			otq = otq && onlyTagsQuery(cn);
		}
		return otq;
	}
	case Node::STRING:
	case Node::STRING_ITEM:
	case Node::STRING_REGION:
	{
		return node->value.size() && node->value.front() == '@';
	}
	default:
		return false;
	};
}

bool hasSpatialQuery(const liboscar::AdvancedCellOpTree::Node * node) {
	using Node = liboscar::AdvancedCellOpTree::Node;
	switch (node->subType) {
	case Node::SET_OP:
	{
		bool hsq = false;
		for(auto cn : node->children) {
			hsq = hsq || hasSpatialQuery(cn);
		}
		return hsq;
	}
	case Node::FM_CONVERSION_OP:
	case Node::CELL_DILATION_OP:
	case Node::REGION_DILATION_OP:
	case Node::COMPASS_OP:
	case Node::BETWEEN_OP:
	case Node::RECT:
	case Node::POLYGON:
	case Node::PATH:
	case Node::POINT:
		return true;
	default:
		return false;
	};
}

bool hasSetOp(const liboscar::AdvancedCellOpTree::Node * node) {
	using Node = liboscar::AdvancedCellOpTree::Node;
	switch (node->subType) {
	case Node::SET_OP:
		return true;
	default:
	{
		bool hso = false;
		for(auto cn : node->children) {
			hso = hso || hasSetOp(cn);
		}
		return hso;
	}
	};
}

bool hasSetDifference(const liboscar::AdvancedCellOpTree::Node * node) {
	using Node = liboscar::AdvancedCellOpTree::Node;
	switch (node->subType) {
	case Node::SET_OP:
		return node->value.size() && node->value.front() == '-';
	default:
	{
		bool hso = false;
		for(auto cn : node->children) {
			hso = hso || hasSetOp(cn);
		}
		return hso;
	}
	};
}

int main(int argc, char ** argv) {
	
	uint32_t threadCount = std::max<uint32_t>(std::thread::hardware_concurrency(), 8);
	
	std::string oscarData;
	bool onlyText = false;
	bool onlyTags = false;
	bool withSpatial = false;
	bool withSetOp = false;
	bool withoutSetOp = false;
	bool withoutSetDifference = false;

	for(int i(1); i < argc; ++i) {
		std::string token(argv[i]);
		if (token == "--help" || token == "-h") {
			help();
			return 0;
		}
		else if (token == "--filter" && i+1 < argc) {
			oscarData = std::string(argv[i+1]);
			++i;
		}
		else if (token == "--only-text") {
			onlyText = true;
		}
		else if (token == "--only-tags") {
			onlyTags = true;
		}
		else if (token == "--with-spatial") {
			withSpatial = true;
		}
		else if (token == "--with-setop") {
			withSetOp = true;
		}
		else if (token == "--no-setop") {
			withoutSetOp = true;
		}
		else if (token == "--no-set-difference") {
			withoutSetDifference = true;
		}
		else {
			std::cerr << "Unknown option: " << token << std::endl;
			return -1;
		}
	}
	
	if (onlyText && withSpatial) {
		std::cout << "Incompatible selectors: --only-text and --with-spatial" << std::endl;
	}
	
	if (withSetOp && withoutSetOp) {
		std::cout << "Incompatible selectors: --with-setop and --no-setop" << std::endl;
	}
	
	std::istream & inFile = std::cin;
	
	liboscar::Static::OsmCompleter completer;
	liboscar::Static::OsmKeyValueObjectStore dummyStore;
	sserialize::Static::ItemIndexStore dummyIdx;
	sserialize::Static::spatial::TracGraph dummyTG;
	sserialize::Static::CQRDilator::CellInfo dummyCI;
	sserialize::Static::CellTextCompleter dummyCtc;
	sserialize::Static::CQRDilator dummyCQRDilator(dummyCI, dummyTG);
	sserialize::spatial::GeoHierarchySubGraph dummyGHSG;
	liboscar::CQRFromPolygon dummycqrP(dummyStore, dummyIdx);
	liboscar::CQRFromComplexSpatialQuery dummyDing(dummyGHSG, dummycqrP);

	liboscar::AdvancedCellOpTree tree(
		dummyCtc, dummyCQRDilator, dummyDing, dummyGHSG
	);
	
	if (oscarData.size()) {
		completer.setAllFilesFromPrefix(oscarData);
		try {
			completer.energize(sserialize::spatial::GeoHierarchySubGraph::T_PASS_THROUGH);
		}
		catch (const std::exception & e) {
			std::cerr << "Could not open oscar data:" << e.what() << std::endl;
			return -1;
		}
	}
	
	uint32_t count = 0;
	while (!inFile.eof()) {
		std::string line;
		std::getline(inFile, line);
		
		tree.parse(line);
		if (!tree.root()) {
			continue;
		}
		if (onlyText && !onlyTextQuery(tree.root())) {
			continue;
		}
		if (onlyTags && !onlyTagsQuery(tree.root())) {
			continue;
		}
		if (withSpatial && !hasSpatialQuery(tree.root())) {
			continue;
		}
		if (withSetOp && !hasSetOp(tree.root())) {
			continue;
		}
		if (withoutSetOp && hasSetOp(tree.root())) {
			continue;
		}
		if (withoutSetDifference && hasSetDifference(tree.root())) {
			continue;
		}
		
		if (oscarData.size() && completer.cqrComplete(line, true, threadCount).cellCount() == 0) {
			continue;
		}
		std::cout << line << '\n';
		++count;
	}
	std::cerr << "Could parse " << count << " queries" << std::endl; 
	return 0;
}
