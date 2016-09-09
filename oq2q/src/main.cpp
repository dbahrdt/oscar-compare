#include <iostream>
#include <liboscar/AdvancedCellOpTree.h>
#include <liboscar/StaticOsmCompleter.h>

//file format is as follows:
//one query per line

struct UnsupportedQuery {};

bool suffixToPrefix = false;

sserialize::XmlEscaper escaper;

std::string toXml(const std::string & str) {
	if (!str.size()) {
		throw UnsupportedQuery();
	}
	std::string qstr;
	if ('!' == str[0] || '#' == str[0]) {
		qstr.insert(qstr.end(), str.begin()+1, str.end());
	}
	else {
		qstr = str;
	}
	
	if (!qstr.size()) {
		throw UnsupportedQuery();
	}
	
	int qt = sserialize::StringCompleter::QT_NONE;
	qt = sserialize::StringCompleter::normalize(qstr);
	
	if (suffixToPrefix && (qt & (sserialize::StringCompleter::QT_SUFFIX | sserialize::StringCompleter::QT_SUBSTRING))) {
		qt |= sserialize::StringCompleter::QT_PREFIX;
		qt &= ~(sserialize::StringCompleter::QT_SUFFIX | sserialize::StringCompleter::QT_SUBSTRING);
	}
	
	bool regionQ = true;
	bool itemQ = true;
	bool tag = false;
	
	if ('!' == str[0]) {
		regionQ = false;
	}
	else if ('#' == str[0]) {
		itemQ = false;
	}
	
	if ('@' == qstr[0]) {
		qstr = std::string(qstr.begin()+1, qstr.end());
		tag = true;
	}
	
	if (!qstr.size()) {
		throw UnsupportedQuery();
	}
	
	qstr = escaper.escape(qstr);
	
	std::stringstream ss;
	ss << '<' << (tag? "tag" : "str")
		<< " region=\"" << (regionQ ? "true"  : "false") << "\""
		<< " item=\"" << (itemQ ? "true"  : "false") << "\""
		<< " prefix=\"" << (qt & (sserialize::StringCompleter::QT_PREFIX | sserialize::StringCompleter::QT_SUBSTRING) ? "true" : "false") << "\""
		<< " suffix=\"" << (qt & (sserialize::StringCompleter::QT_SUFFIX | sserialize::StringCompleter::QT_SUBSTRING)? "true" : "false") << "\""
		<< " exact=\"" << (qt & sserialize::StringCompleter::QT_EXACT ? "true" : "false") << "\""
		<< " q=\"" << qstr << "\"/>";
	return ss.str();
}

//@return depth
uint32_t toXml(const liboscar::AdvancedCellOpTree::Node * node, const std::string & prefix, std::ostream & out, uint32_t & nodes) {
	switch (node->subType) {
	case liboscar::AdvancedCellOpTree::Node::OpType::STRING:
	{
		++nodes;
		out << prefix << toXml(node->value) << "\n";
		return 1;
	}
		break;
	case liboscar::AdvancedCellOpTree::Node::OpType::SET_OP: {
		switch (node->value.front()) {
		case '+':
		{
			++nodes;
			out << prefix << "<or>\n";
			uint32_t d1 = toXml(node->children.front(), prefix + "\t", out, nodes);
			uint32_t d2 = toXml(node->children.back(), prefix + "\t", out, nodes);
			out << prefix << "</or>\n";
			return std::max(d1, d2)+1;
		}
		case '/':
		case ' ':
		{
			++nodes;
			out << prefix << "<and>\n";
			uint32_t d1 = toXml(node->children.front(), prefix + "\t", out, nodes);
			uint32_t d2 = toXml(node->children.back(), prefix + "\t", out, nodes);
			out << prefix << "</and>\n";
			return std::max(d1, d2)+1;
		}
			break;
		case '-':
		{
			++nodes;
			out << prefix << "<diff>\n";
			uint32_t d1 = toXml(node->children.front(), prefix + "\t", out, nodes);
			uint32_t d2 = toXml(node->children.back(), prefix + "\t", out, nodes);
			out << prefix << "</diff>\n";
			return std::max(d1, d2)+1;
		}
			break;
		default:
			throw UnsupportedQuery();
		}
		break;
	}
	default:
		throw UnsupportedQuery();
	}
}

void help() {
	std::cerr << "cat queries.txt | oq2q [--suffix2prefix|-s2p] [--help|-h] [--filter <oscar-data>] > queries.xml" << std::endl;
}

int main(int argc, char ** argv) {
	std::string oscarData;

	for(int i(1); i < argc; ++i) {
		std::string token(argv[i]);
		if (token == "--help" || token == "-h") {
			help();
			return 0;
		}
		else if (token == "--suffix2prefix" || token == "-s2p") {
			suffixToPrefix = true;
		}
		else if (token == "--filter" && i+1 < argc) {
			oscarData = std::string(argv[i+1]);
			++i;
		}
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
	if (oscarData.empty()) {
		std::cout << "<ocse>" << std::endl;
	}
	else {
		std::cout << "<ocse filter=" << escaper.escape(oscarData) << "\"" << std::endl;
	}
	while (!inFile.eof()) {
		std::string line;
		std::getline(inFile, line);
		
		tree.parse(line);
		if (!tree.root()) {
			continue;
		}
		std::stringstream ss;
		uint32_t depth = 0;
		uint32_t nodes = 0;
		try {
			depth = toXml(tree.root(), "\t", ss, nodes);
		}
		catch (const UnsupportedQuery & e) {
			continue;
		}
		
		if (!oscarData.empty() && completer.cqrComplete(line, true, std::max<uint32_t>(std::thread::hardware_concurrency(), 8)).cellCount() == 0) {
			continue;
		}
		
		std::cout << "<query depth=\"" << depth << "\" nodes=\"" << nodes << "\">\n";
		std::cout << ss.str();
		std::cout << "</query>\n";
		++count;
	}
	std::cout << "</ocse>" << std::endl;
	std::cerr << "Could parse " << count << " queries" << std::endl; 
	return 0;
}
