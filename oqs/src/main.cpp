#include <iostream>
#include <liboscar/AdvancedCellOpTree.h>
#include <liboscar/StaticOsmCompleter.h>

//file format is as follows:
//one query per line

struct UnsupportedQuery {};

struct Stats {
	typedef enum {
		TQ_TOTAL,
		TQ_EXACT,
		TQ_PREFIX,
		TQ_SUFFIX,
		TQ_SUBSTRING,
		
		TQ_ITEMS_TOTAL,
		TQ_ITEMS_EXACT,
		TQ_ITEMS_PREFIX,
		TQ_ITEMS_SUFFIX,
		TQ_ITEMS_SUBSTRING,
		
		TQ_REGIONS_TOTAL,
		TQ_REGIONS_EXACT,
		TQ_REGIONS_PREFIX,
		TQ_REGIONS_SUFFIX,
		TQ_REGIONS_SUBSTRING,
		
		SQ_TOTAL,
		SQ_PATH,
		SQ_POLYGON,
		SQ_RECTANGLE,
		SQ_POINT,
		
		CS_TOTAL,
		CS_FM_CONVERSION,
		CS_CELL_DILATION,
		CS_REGION_DILATION,
		CS_BETWEEN,
		CS_COMPASS,
		
		SP_INTERSECT,
		SP_UNITE,
		SP_DIFF,
		SP_SYM_DIFF,
		__TYPES_COUNT
	} Types;
	
	static const std::array<const char*, __TYPES_COUNT> TypeNames;

	std::array<uint32_t, __TYPES_COUNT> counts;
	
	Stats() {
		counts.fill(0);
	}
	
	void count(const Stats & other);
	void accumulate(const Stats & other);
	
	static std::string header();
};

const std::array<const char*, Stats::__TYPES_COUNT> Stats::TypeNames = {{
	"strings-total", "exact", "prefix", "suffix", "substring",
	"item-strings-total", "items-exact", "items-prefix", "items-suffix", "items-substring",
	"region-strings-total", "regions-exact", "regions-prefix", "regions-suffix", "regions-substring",
	"spatial-total", "path", "polygon", "rectangle", "point",
	"complex-total", "fm-conversion", "cell-dilation", "region-dilation", "between", "compass",
	"intersect", "unite", "diff", "sym-diff"
}};

void Stats::count(const Stats& other) {
	for(std::size_t i(0); i < counts.size(); ++i) {
		if (other.counts.at(i)) {
			counts.at(i) += 1;
		}
	}
}

void Stats::accumulate(const Stats& other) {
	for(std::size_t i(0); i < counts.size(); ++i) {
		counts.at(i) += other.counts.at(i);
	}
}

std::string Stats::header() {
	std::stringstream ss;
	for(std::size_t i(0); i < __TYPES_COUNT-1; ++i) {
		ss << TypeNames[i] << ";";
	}
	ss << TypeNames.back();
	return ss.str();
}

std::ostream & operator<<(std::ostream & out, Stats const & stat) {
	for(std::size_t i(0); i < stat.counts.size()-1; ++i) {
		out << stat.counts.at(i) << ';';
	}
	out << stat.counts.back();
	return out;
}

void updateStat(const liboscar::AdvancedCellOpTree::Node * node, Stats & stat) {
	using Node = liboscar::AdvancedCellOpTree::Node;
	switch (node->subType) {
	case Node::SET_OP:
	{
		for(auto node : node->children) {
			updateStat(node, stat);
		}
		switch (node->value.front()) {
		case '+':
			stat.counts.at(Stats::SP_UNITE) += 1;
			break;
		case '/':
		case ' ':
			stat.counts.at(Stats::SP_INTERSECT) += 1;
			break;
		case '-':
			stat.counts.at(Stats::SP_DIFF) += 1;
			break;
		case '^':
			stat.counts.at(Stats::SP_SYM_DIFF) += 1;
			break;
		}
		break;
	}
	case Node::STRING:
	{
		stat.counts.at(Stats::TQ_TOTAL) += 1;
		std::string qstr(node->value);
		sserialize::StringCompleter::QuerryType qt = sserialize::StringCompleter::QT_NONE;
		qt = sserialize::StringCompleter::normalize(qstr);
		if ((qt & sserialize::StringCompleter::QT_SUBSTRING) == sserialize::StringCompleter::QT_SUBSTRING) {
			stat.counts.at(Stats::TQ_SUBSTRING) += 1;
		}
		else if ((qt & sserialize::StringCompleter::QT_SUFFIX) == sserialize::StringCompleter::QT_SUFFIX) {
			stat.counts.at(Stats::TQ_SUFFIX) += 1;
		}
		else if ((qt & sserialize::StringCompleter::QT_PREFIX) == sserialize::StringCompleter::QT_PREFIX) {
			stat.counts.at(Stats::TQ_PREFIX) += 1;
		}
		else if ((qt & sserialize::StringCompleter::QT_EXACT) == sserialize::StringCompleter::QT_EXACT) {
			stat.counts.at(Stats::TQ_EXACT) += 1;
		}
		break;
	}
	case Node::STRING_ITEM:
	{
		stat.counts.at(Stats::TQ_ITEMS_TOTAL) += 1;
		std::string qstr(node->value);
		sserialize::StringCompleter::QuerryType qt = sserialize::StringCompleter::QT_NONE;
		qt = sserialize::StringCompleter::normalize(qstr);
		if ((qt & sserialize::StringCompleter::QT_SUBSTRING) == sserialize::StringCompleter::QT_SUBSTRING) {
			stat.counts.at(Stats::TQ_ITEMS_SUBSTRING) += 1;
		}
		else if ((qt & sserialize::StringCompleter::QT_SUFFIX) == sserialize::StringCompleter::QT_SUFFIX) {
			stat.counts.at(Stats::TQ_ITEMS_SUFFIX) += 1;
		}
		else if ((qt & sserialize::StringCompleter::QT_PREFIX) == sserialize::StringCompleter::QT_PREFIX) {
			stat.counts.at(Stats::TQ_ITEMS_PREFIX) += 1;
		}
		else if ((qt & sserialize::StringCompleter::QT_EXACT) == sserialize::StringCompleter::QT_EXACT) {
			stat.counts.at(Stats::TQ_ITEMS_EXACT) += 1;
		}
		break;
	}
	case Node::STRING_REGION:
	{
		stat.counts.at(Stats::TQ_REGIONS_TOTAL) += 1;
		std::string qstr(node->value);
		sserialize::StringCompleter::QuerryType qt = sserialize::StringCompleter::QT_NONE;
		qt = sserialize::StringCompleter::normalize(qstr);
		if ((qt & sserialize::StringCompleter::QT_SUBSTRING) == sserialize::StringCompleter::QT_SUBSTRING) {
			stat.counts.at(Stats::TQ_SUBSTRING) += 1;
		}
		else if ((qt & sserialize::StringCompleter::QT_SUFFIX) == sserialize::StringCompleter::QT_SUFFIX) {
			stat.counts.at(Stats::TQ_REGIONS_SUFFIX) += 1;
		}
		else if ((qt & sserialize::StringCompleter::QT_PREFIX) == sserialize::StringCompleter::QT_PREFIX) {
			stat.counts.at(Stats::TQ_REGIONS_PREFIX) += 1;
		}
		else if ((qt & sserialize::StringCompleter::QT_EXACT) == sserialize::StringCompleter::QT_EXACT) {
			stat.counts.at(Stats::TQ_REGIONS_EXACT) += 1;
		}
		break;
	}
	case Node::RECT:
		stat.counts.at(Stats::SQ_TOTAL) += 1;
		stat.counts.at(Stats::SQ_RECTANGLE) += 1;
		break;
	case Node::POLYGON:
		stat.counts.at(Stats::SQ_TOTAL) += 1;
		stat.counts.at(Stats::SQ_POLYGON) += 1;
		break;
	case Node::PATH:
		stat.counts.at(Stats::SQ_TOTAL) += 1;
		stat.counts.at(Stats::SQ_PATH) += 1;
		break;
	case Node::POINT:
		stat.counts.at(Stats::SQ_TOTAL) += 1;
		stat.counts.at(Stats::SQ_POINT) += 1;
		break;
	case Node::FM_CONVERSION_OP:
		stat.counts.at(Stats::CS_TOTAL) += 1;
		stat.counts.at(Stats::CS_FM_CONVERSION) += 1;
		break;
	case Node::CELL_DILATION_OP:
		stat.counts.at(Stats::CS_TOTAL) += 1;
		stat.counts.at(Stats::CS_CELL_DILATION) += 1;
		break;
	case Node::REGION_DILATION_OP:
		stat.counts.at(Stats::CS_TOTAL) += 1;
		stat.counts.at(Stats::CS_REGION_DILATION) += 1;
		break;
	case Node::COMPASS_OP:
		stat.counts.at(Stats::CS_TOTAL) += 1;
		stat.counts.at(Stats::CS_COMPASS) += 1;
		break;
	case Node::BETWEEN_OP:
		stat.counts.at(Stats::CS_TOTAL) += 1;
		stat.counts.at(Stats::CS_BETWEEN) += 1;
		break;
	default:
		return;
	};
}

void help() {
	std::cerr << "oqs -f <infile> -o <per query stats file>" << std::endl;
}

int main(int argc, char ** argv) {
	std::string inputFile, outputFile;

	for(int i(1); i < argc; ++i) {
		std::string token(argv[i]);
		if (token == "--help" || token == "-h") {
			help();
			return 0;
		}
		else if (token == "-f" && i+1 < argc) {
			inputFile = std::string(argv[i+1]);
			++i;
		}
		else if (token == "-o" && i+1 < argc) {
			outputFile = std::string(argv[i+1]);
			++i;
		}
		else {
			std::cerr << "Unkown parameter: " << token << std::endl;
			help();
			return -1;
		}
	}
	
	
	std::ifstream inFile;
	inFile.open(inputFile);
	if (!inFile.is_open()) {
		std::cerr << "Could not open input file " << inputFile << std::endl;
		return -1;
	}
	
	std::ofstream outFile;
	outFile.open(outputFile);
	if (!outFile.is_open()) {
		std::cerr << "Could not open output file " << outputFile << std::endl;
		return -1;
	}
	
	outFile << "Query id;" << Stats::header() << '\n';
	
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
	
	Stats counts;
	Stats acc;
	uint32_t totalCount = 0;
	while (!inFile.eof()) {
		std::string line;
		std::getline(inFile, line);
		
		tree.parse(line);
		if (!tree.root()) {
			continue;
		}
		
		Stats stat;
		updateStat(tree.root(), stat);
		counts.count(stat);
		acc.accumulate(stat);
		outFile << totalCount << ';' << stat << '\n';
		++totalCount;
	}
	std::cout << "Total number of queries: " << totalCount << std::endl;
	std::cout << "Number of queries with any of the following statements:\n";
	for(std::size_t i(0); i < counts.counts.size(); ++i) {
		std::cout << Stats::TypeNames.at(i) << ": " << uint32_t(counts.counts.at(i)) << std::endl;
	}
	std::cout << "Total number of query statements:" << std::endl;
	for(std::size_t i(0); i < acc.counts.size(); ++i) {
		std::cout << Stats::TypeNames.at(i) << ": " << acc.counts.at(i) << '\n';
	}
	std::cout << std::flush;
	
	return 0;
}
