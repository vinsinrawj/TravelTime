import React from 'react';

import HotelSearchForm from '../components/hotelSearchForm.jsx';
import HotelSearchResult from '../components/hotelsList.jsx';
import * as HotelsActions from '../actions/HotelsActions.jsx';
import HotelsStore from '../stores/HotelsStore.jsx';

var HotelSearchPanel = React.createClass({
  getInitialState: function() {
    return {
      hotels: HotelsStore.getAllHotels()
    };
  },
  // update search result

  componentWillMount: function() {
    HotelsStore.on("change", this.getHotels);
  },

  componentWillUnmount: function() {
    HotelsStore.removeListener("change", this.getHotels);
  },

  getHotels: function() {
    this.setState({
      hotels: HotelsStore.getAllHotels(),
    });
  },

  reloadHotels: function(requestJSON) {
    HotelsActions.reloadHotels(requestJSON);
  },

  render: function () {
    if(this.state.hotels) {
      var SearchResultOption = <HotelSearchResult searchResult={this.state.hotels}></HotelSearchResult>
    } else {
      var SearchResultOption = <div className="circular"></div>
    }
    return(
      <div className="mdl-grid">
        <HotelSearchForm updateSearchResult = {this.reloadHotels}></HotelSearchForm>
        {SearchResultOption}
      </div>
    );
  }
});
export default HotelSearchPanel;