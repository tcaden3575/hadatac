FROM r-base:latest


###################################
# System libraries of general use #
###################################

# The /var/lib/apt/lists stores the package information cache, which can be removed after installations
RUN apt-get update && apt-get install --no-install-recommends -y \
    libcurl4-gnutls-dev \
    libcairo2-dev \
    libxt-dev \
    libssl-dev \
    libxml2-dev \
    libsodium-dev \
    pandoc \
    && rm -rf /var/lib/apt/lists/*

# basic shiny functionality
#RUN R -e "install.packages(c('shiny', 'rmarkdown'), repos='https://cloud.r-project.org/')"
RUN R -e 'install.packages(c("shiny"))'
#RUN R -e "install.packages(c('rmarkdown'), repos='https://cloud.r-project.org/')"
#RUN R -e 'install.packages(c("rmarkdown")'

#RUN R -e 'install.packages(c("tidyverse"))'
RUN R -e 'install.packages(c("tidyr"))'
RUN R -e 'install.packages(c("dplyr"))'
RUN R -e 'install.packages(c("plotly"))'
RUN R -e 'install.packages(c("ggplot2"))'
RUN R -e 'install.packages(c("flexdashboard"))'
RUN R -e 'install.packages(c("DT"))'

RUN echo "local(options(shiny.port = 3838, shiny.host = '0.0.0.0'))" > /usr/lib/R/etc/Rprofile.site

#RUN addgroup --system app \
#    && adduser --system --ingroup app app
WORKDIR /home/app/2017-1762

# copy the app to the image
COPY data .

#COPY Rprofile.site /usr/lib/R/etc/

#RUN chown app:app -R /home/app
#USER app
EXPOSE 3838

CMD ["R", "-e", "rmarkdown::run('/home/app/2017-1762/dashboard1.Rmd')"]
